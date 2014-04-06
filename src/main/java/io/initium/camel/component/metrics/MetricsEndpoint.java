package io.initium.camel.component.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.CachedGauge;
import com.codahale.metrics.Counter;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.SlidingTimeWindowReservoir;
import com.codahale.metrics.Timer;

import io.initium.common.util.StringUtils;

import static io.initium.camel.component.metrics.MetricsComponent.DEFALUT_JMX_REPORTER_DOMAIN;
import static io.initium.camel.component.metrics.MetricsComponent.MARKER;

// TODO add more customizable reservoirs
// TODO add support for ratio gauges
// TODO make default types of metrics configurable
// TODO make default exposed TimeUnits configurable

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.0
 * @since 2014-02-19
 */
public class MetricsEndpoint extends DefaultEndpoint {

	/**
	 * 
	 */
	public enum TimingAction {
		NOOP, // no operation
		START, // start timers
		STOP; // stop timers
	}

	/**
	 *
	 */
	private enum ReservoirType {
		DEFAULT,
		SLIDING_TIME_WINDOW;

		public static ReservoirType find(final String name) {
			for (ReservoirType reservoirType : ReservoirType.values()) {
				if (reservoirType.name().replaceAll("_", "").equalsIgnoreCase(name)) {
					return reservoirType;
				}
			}
			return null;
		}

	}

	// logging
	private static final String				SELF							= Thread.currentThread().getStackTrace()[1].getClassName();
	private static final Logger				LOGGER							= LoggerFactory.getLogger(SELF);

	// fields
	private final String					name;
	private final MetricRegistry			metricRegistry;
	private final Map<TimeUnit, Histogram>	intervals						= new HashMap<TimeUnit, Histogram>();
	private JmxReporter						jmxReporter;
	private long							lastExchangeTime				= System.nanoTime();
	private Meter							exchangeRate;
	private Timer							internalTimer					= null;
	private String							timing;
	private TimingAction					timingAction					= TimingAction.NOOP;
	private boolean							isFirstStart					= true;
	private Timer							timer							= null;
	private Expression						counterDelta					= null;
	private Counter							counter;
	private Expression						histogramValue					= null;
	private Histogram						histogram;
	private Expression						gaugeValue						= null;
	private Exchange						lastExchange;
	private String							context							= DEFALUT_JMX_REPORTER_DOMAIN;
	private TimeUnit						durationUnit					= TimeUnit.MILLISECONDS;
	private TimeUnit						rateUnit						= TimeUnit.SECONDS;
	private boolean							isInternalTimerEnabled			= false;
	private long							gaugeCacheDuration				= 10;
	private TimeUnit						gaugeCacheDurationUnit			= TimeUnit.SECONDS;
	private ReservoirType					histogramReservoirType			= ReservoirType.DEFAULT;
	private long							slidingTimeWindowDuration		= 5;
	private TimeUnit						slidingTimeWindowDurationUnit	= TimeUnit.MINUTES;
	private String							timingName						= "timing";
	private String							counterName						= "count";
	private String							histogramName					= "histogram";
	private String							gaugeName						= "gauge";

	/**
	 * @param uri
	 * @param component
	 * @param name
	 * @param parameters
	 */
	public MetricsEndpoint(final String uri, final MetricsComponent component, final String name, final Map<String, Object> parameters) {
		super(uri, component);
		LOGGER.debug(MARKER, "MetricsEndpoint({},{},{})", uri, component, name);
		this.name = name;
		this.metricRegistry = new MetricRegistry();
	}

	@Override
	public Consumer createConsumer(final Processor processor) throws Exception {
		LOGGER.debug(MARKER, "createConsumer({})", processor);
		Consumer consumer = new MetricsConsumer(this, processor);
		configureConsumer(consumer);
		return consumer;
	}

	@Override
	public Producer createProducer() throws Exception {
		LOGGER.debug(MARKER, "createProducer({})");
		final MetricsProducer producer = new MetricsProducer(this);
		return producer;
	};

	/**
	 * @return the context
	 */
	public String getContext() {
		return this.context;
	}

	/**
	 * @return the counter
	 */
	public Counter getCounter() {
		return this.counter;
	}

	/**
	 * @return the counter
	 */
	public Expression getCounterDelta() {
		return this.counterDelta;
	}

	/**
	 * @return the durationUnit
	 */
	public TimeUnit getDurationUnit() {
		return this.durationUnit;
	}

	/**
	 * @return the exchangeRateMetric
	 */
	public Meter getExchangeRate() {
		return this.exchangeRate;
	}

	/**
	 * @return the histogram
	 */
	public Histogram getHistogram() {
		return this.histogram;
	}

	/**
	 * @return the histogramValue
	 */
	public Expression getHistogramValue() {
		return this.histogramValue;
	}

	/**
	 * @return
	 */
	public Timer getInternalTimer() {
		return this.internalTimer;
	}

	/**
	 * @return
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return the rateUnit
	 */
	public TimeUnit getRateUnit() {
		return this.rateUnit;
	}

	/**
	 * @return
	 */
	public Timer getTimer() {
		return this.timer;
	}

	/**
	 * @return the timing
	 */
	public String getTiming() {
		return this.timing;
	}

	/**
	 * @return
	 */
	public TimingAction getTimingAction() {
		return this.timingAction;
	}

	/**
	 * @return the isInternalTimerEnabled
	 */
	public boolean isInternalTimerEnabled() {
		return this.isInternalTimerEnabled;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	/**
	 * @param exchange
	 */
	public void mark(final Exchange exchange) {
		long deltaInNanos = lastExchangeDelta();
		this.lastExchangeTime = System.nanoTime();
		this.exchangeRate.mark();
		updateAllIntervals(deltaInNanos);
	}

	/**
	 * @param context
	 *            the context to set
	 */
	public void setContext(final String context) {
		this.context = context;
	}

	/**
	 * @param counter
	 *            the counter to set
	 */
	public void setCounter(final Counter counter) {
		this.counter = counter;
	}

	/**
	 * @param counterDelta
	 *            the counterDelta to set
	 */
	public void setCounterDelta(final String counterDelta) {
		this.counterDelta = createFileLanguageExpression(counterDelta);
	}

	/**
	 * @param counterName
	 *            the counterName to set
	 */
	public void setCounterName(final String counterName) {
		this.counterName = counterName;
	}

	/**
	 * @param durationUnitName
	 *            the durationUnitName to set
	 */
	public void setDurationUnit(final String durationUnitName) {
		this.durationUnit = TimeUnit.valueOf(durationUnitName.toUpperCase());
	}

	/**
	 * @param internalTimerEnabled
	 *            the internalTimerEnabled to set
	 */
	public void setEnableInternalTimer(final String internalTimerEnabled) {
		if ("1".equals(internalTimerEnabled)) {
			this.isInternalTimerEnabled = true;
		} else if ("yes".equalsIgnoreCase(internalTimerEnabled)) {
			this.isInternalTimerEnabled = true;
		} else if ("ja".equalsIgnoreCase(internalTimerEnabled)) {
			this.isInternalTimerEnabled = true;
		} else if ("si".equalsIgnoreCase(internalTimerEnabled)) {
			this.isInternalTimerEnabled = true;
		} else {
			this.isInternalTimerEnabled = Boolean.parseBoolean(internalTimerEnabled);
		}
	}

	/**
	 * @param gaugeCacheDuration
	 *            the gaugeCacheDuration to set
	 */
	public void setGaugeCacheDuration(final String gaugeCacheDuration) {
		long duration = Long.parseLong(gaugeCacheDuration);
		this.gaugeCacheDuration = duration;
	}

	/**
	 * @param gaugeCacheDurationUnitName
	 *            the gaugeCacheDurationUnit to set
	 */
	public void setGaugeCacheDurationUnit(final String gaugeCacheDurationUnitName) {
		this.gaugeCacheDurationUnit = TimeUnit.valueOf(gaugeCacheDurationUnitName.toUpperCase());
	}

	/**
	 * @param gaugeName
	 *            the gaugeName to set
	 */
	public void setGaugeName(final String gaugeName) {
		this.gaugeName = gaugeName;
	}

	/**
	 * @param gaugeValue
	 *            the gaugeValueDelta to set
	 */
	public void setGaugeValue(final String gaugeValue) {
		this.gaugeValue = createFileLanguageExpression(gaugeValue);
	}

	/**
	 * @param histogramName
	 *            the histogramName to set
	 */
	public void setHistogramName(final String histogramName) {
		this.histogramName = histogramName;
	}

	/**
	 * @param reservoirType
	 *            the histogramReservoirType to set
	 */
	public void setHistogramReservoir(final String reservoirType) {
		this.histogramReservoirType = ReservoirType.find(reservoirType);
	}

	/**
	 * @param histogramValue
	 *            the histogramValueDelta to set
	 */
	public void setHistogramValue(final String histogramValue) {
		this.histogramValue = createFileLanguageExpression(histogramValue);
	}

	/**
	 * @param lastExchange
	 */
	public void setLastExchange(final Exchange lastExchange) {
		this.lastExchange = lastExchange;
	}

	/**
	 * @param rateUnitName
	 *            the rateUnitName to set
	 */
	public void setRateUnit(final String rateUnitName) {
		this.rateUnit = TimeUnit.valueOf(rateUnitName.toUpperCase());
	}

	/**
	 * @param slidingTimeWindowDuration
	 *            the slidingTimeWindowDuration to set
	 */
	public void setSlidingTimeWindowDuration(final String slidingTimeWindowDuration) {
		long duration = Long.parseLong(slidingTimeWindowDuration);
		this.slidingTimeWindowDuration = duration;
	}

	/**
	 * @param slidingTimeWindowDurationUnit
	 *            the slidingTimeWindowDurationUnit to set
	 */
	public void setSlidingTimeWindowDurationUnit(final String slidingTimeWindowDurationUnit) {
		this.slidingTimeWindowDurationUnit = TimeUnit.valueOf(slidingTimeWindowDurationUnit.toUpperCase());
	}

	/**
	 * @param timing
	 *            the timing to set
	 */
	public void setTiming(final String timing) {
		this.timing = timing;
		this.timingAction = TimingAction.valueOf(timing.toUpperCase());
	}

	/**
	 * @param timingName
	 *            the timingName to set
	 */
	public void setTimingName(final String timingName) {
		this.timingName = timingName;
	}

	private Expression createFileLanguageExpression(final String expression) {
		Language language;
		// only use file language if the name is complex (i.e. contains "$")
		if (expression.contains("$")) {
			language = getCamelContext().resolveLanguage("file");
		} else {
			language = getCamelContext().resolveLanguage("constant");
		}
		return language.createExpression(expression);
	}

	/**
	 * 
	 */
	private void doFirstStartIfNeeded() {
		LOGGER.debug(MARKER, "doFirstStartIfNeeded()");
		LOGGER.debug(MARKER, "isFirstStart={}, this.timingAction={}", this.isFirstStart, this.timingAction);
		if (!this.isFirstStart || this.timingAction == TimingAction.STOP) {
			LOGGER.debug(MARKER, "skipping first startup");
			return;
		}
		LOGGER.debug(MARKER, "not skipping first startup");

		this.isFirstStart = false;

		// jmx reporting
		//@formatter:off
		this.jmxReporter = JmxReporter
				.forRegistry(this.metricRegistry)
				.inDomain(this.context)
				.convertDurationsTo(this.durationUnit)
				.convertRatesTo(this.rateUnit) 
				.build();
		//@formatter:on

		// Exchange Rate
		String exchangeRateMetricName = MetricRegistry.name(this.name, "exchangeRate");
		this.exchangeRate = this.metricRegistry.meter(exchangeRateMetricName);

		// Since Metrics
		for (final TimeUnit timeUnit : TimeUnit.values()) {
			String sinceName = MetricRegistry.name(this.name, "since" + getPrettyName(timeUnit));
			this.metricRegistry.register(sinceName, new Gauge<Double>() {
				@Override
				public Double getValue() {
					return lastExchangeDelta(timeUnit);
				}
			});
		}

		// Interval Metrics
		for (final TimeUnit timeUnit : TimeUnit.values()) {
			String intervalName = MetricRegistry.name(this.name, "exchangeInterval" + getPrettyName(timeUnit));
			Histogram histogram = this.metricRegistry.histogram(intervalName);
			this.intervals.put(timeUnit, histogram);
		}

		// Timing Metrics
		if (this.timingAction == TimingAction.START) {
			String lclName = MetricRegistry.name(this.name, this.timingName);
			LOGGER.debug(MARKER, "enabling timing metrics: {}", lclName);
			this.timer = this.metricRegistry.timer(lclName);
		}

		// Counter Metrics
		if (this.counterDelta != null) {
			String lclName = MetricRegistry.name(this.name, this.counterName);
			LOGGER.debug(MARKER, "enabling counter metrics: {}", lclName);
			this.counter = this.metricRegistry.counter(lclName);
		}

		// Histogram Metrics
		if (this.histogramValue != null) {
			String lclName = MetricRegistry.name(this.name, this.histogramName);
			LOGGER.debug(MARKER, "enabling histogram metrics: {}", lclName);
			Reservoir reservoir;
			switch (this.histogramReservoirType) {
				case SLIDING_TIME_WINDOW:
					LOGGER.info(MARKER, "using sliding time window: {} {}", this.slidingTimeWindowDuration, this.slidingTimeWindowDurationUnit.toString().toLowerCase());
					reservoir = new SlidingTimeWindowReservoir(this.slidingTimeWindowDuration, this.slidingTimeWindowDurationUnit);
					break;
				default:
					reservoir = new ExponentiallyDecayingReservoir();
					break;
			}
			this.histogram = new Histogram(reservoir);
			this.metricRegistry.register(lclName, this.histogram);
		}

		// Gauge Metrics (Cached By Default)
		if (this.gaugeValue != null) {
			String lclName = MetricRegistry.name(this.name, this.gaugeName);
			LOGGER.debug(MARKER, "enabling gauge metrics: {}", lclName);
			this.metricRegistry.register(lclName, new CachedGauge<Object>(this.gaugeCacheDuration, this.gaugeCacheDurationUnit) {
				@Override
				protected Object loadValue() {
					if (MetricsEndpoint.this.lastExchange != null) {
						return MetricsEndpoint.this.gaugeValue.evaluate(MetricsEndpoint.this.lastExchange, Object.class);
					} else {
						return null;
					}
				}
			});

		}

		// Internal Timer
		if (this.isInternalTimerEnabled) {
			String internalTimerMetricName = MetricRegistry.name(this.name, "internalTiming");
			this.internalTimer = this.metricRegistry.timer(internalTimerMetricName);
		}
	}

	private String getPrettyName(final TimeUnit timeUnit) {
		return StringUtils.capitalize(timeUnit.toString());
	}

	/**
	 * @return
	 */
	private long lastExchangeDelta() {
		return System.nanoTime() - MetricsEndpoint.this.lastExchangeTime;
	}

	/**
	 * @return
	 */
	private double lastExchangeDelta(final TimeUnit timeUnit) {
		return (double) lastExchangeDelta() / timeUnit.toNanos(1);
	}

	/**
	 * @param deltaInNanos
	 */
	private void updateAllIntervals(final long deltaInNanos) {
		for (Entry<TimeUnit, Histogram> entry : this.intervals.entrySet()) {
			long delta = entry.getKey().convert(deltaInNanos, TimeUnit.NANOSECONDS);
			entry.getValue().update(delta);
		}
	}

	@Override
	protected void doResume() throws Exception {
		super.doResume();
		LOGGER.debug(MARKER, "doResume()");
	}

	@Override
	protected void doShutdown() throws Exception {
		super.doShutdown();
		LOGGER.debug(MARKER, "doShutdown()");
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();
		LOGGER.debug(MARKER, "doStart()");
		doFirstStartIfNeeded();
		if (this.jmxReporter != null) {
			this.jmxReporter.start();
		}
	}

	@Override
	protected void doStop() throws Exception {
		super.doStop();
		LOGGER.debug(MARKER, "doStop()");
		if (this.jmxReporter != null) {
			this.jmxReporter.stop();
		}
	}

	@Override
	protected void doSuspend() throws Exception {
		super.doSuspend();
		LOGGER.debug(MARKER, "doSuspend()");
	}

}
