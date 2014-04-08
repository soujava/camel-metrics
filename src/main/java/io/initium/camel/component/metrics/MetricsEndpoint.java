// @formatter:off
/**
 * Copyright 2014 Initium.io
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
// @formatter:on
package io.initium.camel.component.metrics;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.NoSuchBeanException;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.Language;
import org.apache.camel.util.CamelContextHelper;
import org.apache.camel.util.EndpointHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.CachedGauge;
import com.codahale.metrics.ConsoleReporter;
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
import io.initium.common.util.TimeUnitUtils;

import static io.initium.camel.component.metrics.MetricsComponent.DEFAULT_CONTEXT;
import static io.initium.camel.component.metrics.MetricsComponent.MARKER;

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

	// logging
	private static final String				SELF											= Thread.currentThread().getStackTrace()[1].getClassName();
	private static final Logger				LOGGER											= LoggerFactory.getLogger(SELF);

	// fields
	private final String					name;
	private String							context											= DEFAULT_CONTEXT;
	private final MetricRegistry			metricRegistry;
	private final Map<TimeUnit, Histogram>	intervals										= new HashMap<TimeUnit, Histogram>();
	private JmxReporter						jmxReporter;
	private long							lastExchangeTime								= System.nanoTime();
	private Meter							exchangeRate;
	private Timer							internalTimer									= null;
	private String							timing;
	private TimingAction					timingAction									= TimingAction.NOOP;
	private Timer							timer											= null;
	private Expression						counterDelta									= null;
	private Counter							counter;
	private Expression						histogramValue									= null;
	private Histogram						histogram;
	private Expression						gaugeValue										= null;
	private Exchange						lastExchange;
	private TimeUnit						jmxDurationUnit									= TimeUnit.MILLISECONDS;
	private TimeUnit						jmxRateUnit										= TimeUnit.SECONDS;
	private boolean							isInternalTimerEnabled							= false;
	private long							gaugeCacheDuration								= 10;
	private TimeUnit						gaugeCacheDurationUnit							= TimeUnit.SECONDS;
	private String							timingName										= "timing";
	private String							counterName										= "count";
	private String							histogramName									= "histogram";
	private String							gaugeName										= "gauge";
	private Reservoir						intervalReservoir								= new ExponentiallyDecayingReservoir();
	private Reservoir						histogramReservoir								= new ExponentiallyDecayingReservoir();
	private boolean							isJmxReportingEnabled							= true;

	private Reservoir						timingReservoir									= new ExponentiallyDecayingReservoir();
	private long							timingReservoirSlidingTimeWindowDuration		= 1;
	private TimeUnit						timingReservoirSlidingTimeWindowDurationUnit	= TimeUnit.HOURS;
	private final boolean					isConsoleReportingEnabled						= false;
	private ConsoleReporter					consoleReporter;
	private final TimeUnit					consoleDurationUnit								= TimeUnit.MILLISECONDS;
	private final TimeUnit					consoleRateUnit									= TimeUnit.SECONDS;
	private final long						consoleReporterPeriod							= 1;
	private final TimeUnit					consoleReporterPeriodUnit						= TimeUnit.MINUTES;

	/**
	 * @param uri
	 * @param component
	 * @param name
	 * @param parameters
	 * @throws Exception
	 */
	public MetricsEndpoint(final String uri, final MetricsComponent component, final String name, final Map<String, Object> parameters) throws Exception {
		super(uri, component);
		LOGGER.debug(MARKER, "MetricsEndpoint({},{},{})", uri, component, parameters);
		EndpointHelper.setProperties(getCamelContext(), this, parameters);
		this.name = name;
		this.metricRegistry = new MetricRegistry();
		switch (this.timingAction) {
			case STOP:
				LOGGER.debug(MARKER, "initializeMetrics, timingAction={}", this.timingAction);
				break;
			default:
				initializeMetrics();
				initializeReporters();
				break;
		}
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
	 * @return the jmxDurationUnit
	 */
	public TimeUnit getJmxDurationUnit() {
		return this.jmxDurationUnit;
	}

	/**
	 * @return the jmxRateUnit
	 */
	public TimeUnit getJmxRateUnit() {
		return this.jmxRateUnit;
	}

	/**
	 * @return
	 */
	public String getName() {
		return this.name;
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
	 * @return the timingName
	 */
	public String getTimingName() {
		return this.timingName;
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
		this.lastExchange = exchange;
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
	 * @param enableJmxReporting
	 */
	public void setEnableJmxReporting(final String enableJmxReporting) {
		if ("1".equals(enableJmxReporting)) {
			this.isJmxReportingEnabled = true;
		} else if ("yes".equalsIgnoreCase(enableJmxReporting)) {
			this.isJmxReportingEnabled = true;
		} else if ("ja".equalsIgnoreCase(enableJmxReporting)) {
			this.isJmxReportingEnabled = true;
		} else if ("si".equalsIgnoreCase(enableJmxReporting)) {
			this.isJmxReportingEnabled = true;
		} else {
			this.isJmxReportingEnabled = Boolean.parseBoolean(enableJmxReporting);
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
		this.gaugeCacheDurationUnit = TimeUnitUtils.getTimeUnit(gaugeCacheDurationUnitName);
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

	// /**
	// * @param reservoirType
	// * the histogramReservoirType to set
	// */
	// public void setHistogramReservoir(final String reservoirType) {
	// this.histogramReservoirType = ReservoirType.find(reservoirType);
	// }

	/**
	 * @param histogramReservoirName
	 *            the histogramReservoir to set
	 */
	public void setHistogramReservoir(final String histogramReservoirName) {
		this.histogramReservoir = CamelContextHelper.mandatoryLookup(getCamelContext(), histogramReservoirName, Reservoir.class);
	}

	/**
	 * @param histogramValue
	 *            the histogramValueDelta to set
	 */
	public void setHistogramValue(final String histogramValue) {
		this.histogramValue = createFileLanguageExpression(histogramValue);
	}

	/**
	 * @param intervalReservoirName
	 *            the intervalReservoir to set
	 */
	public void setIntervalReservoir(final String intervalReservoirName) {
		this.intervalReservoir = CamelContextHelper.mandatoryLookup(getCamelContext(), intervalReservoirName, Reservoir.class);
	}

	// /**
	// * @param slidingTimeWindowDuration
	// * the slidingTimeWindowDuration to set
	// */
	// public void setSlidingTimeWindowDuration(final String slidingTimeWindowDuration) {
	// long duration = Long.parseLong(slidingTimeWindowDuration);
	// this.slidingTimeWindowDuration = duration;
	// }
	//
	// /**
	// * @param slidingTimeWindowDurationUnit
	// * the slidingTimeWindowDurationUnit to set
	// */
	// public void setSlidingTimeWindowDurationUnit(final String slidingTimeWindowDurationUnit) {
	// this.slidingTimeWindowDurationUnit = TimeUnit.valueOf(slidingTimeWindowDurationUnit.toUpperCase());
	// }

	/**
	 * @param jmxDurationUnitName
	 *            the durationUnitName to set
	 */
	public void setJmxDurationUnit(final String jmxDurationUnitName) {
		this.jmxDurationUnit = TimeUnitUtils.getTimeUnit(jmxDurationUnitName);
		this.jmxDurationUnit = TimeUnitUtils.getTimeUnit(jmxDurationUnitName);
	}

	/**
	 * @param jmxRateUnitName
	 *            the rateUnitName to set
	 */
	public void setJmxRateUnit(final String jmxRateUnitName) {
		this.jmxRateUnit = TimeUnitUtils.getTimeUnit(jmxRateUnitName);
	}

	/**
	 * @param lastExchange
	 */
	public void setLastExchange(final Exchange lastExchange) {
		this.lastExchange = lastExchange;
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

	/**
	 * @param timingReservoirName
	 *            the timingReservoir to set
	 */
	public void setTimingReservoir(final String timingReservoirName) {
		if (timingReservoirName != null && timingReservoirName.length() > 0) {
			char firstChar = timingReservoirName.charAt(0);
			if (firstChar == '#') {
				this.timingReservoir = CamelContextHelper.mandatoryLookup(getCamelContext(), timingReservoirName, Reservoir.class);
			} else {
				if ("SlidingTimeWindow".equalsIgnoreCase(timingReservoirName)) {
					this.timingReservoir = new SlidingTimeWindowReservoir(this.timingReservoirSlidingTimeWindowDuration, this.timingReservoirSlidingTimeWindowDurationUnit);
				}
			}
			if (this.timingReservoir == null) {
				throw new NoSuchBeanException(this.name);
			}
		}
	}

	/**
	 * @param timingReservoirSlidingTimeWindowDuration
	 *            the timingReservoirSlidingTimeWindowDuration to set
	 */
	public void setTimingReservoirSlidingTimeWindowDuration(final String timingReservoirSlidingTimeWindowDuration) {
		this.timingReservoirSlidingTimeWindowDuration = Long.parseLong(timingReservoirSlidingTimeWindowDuration);
	}

	/**
	 * @param timingReservoirSlidingTimeWindowDurationUnitName
	 *            the timingReservoirSlidingTimeWindowDurationUnit to set
	 */
	public void setTimingReservoirSlidingTimeWindowDurationUnit(final String timingReservoirSlidingTimeWindowDurationUnitName) {
		this.timingReservoirSlidingTimeWindowDurationUnit = TimeUnitUtils.getTimeUnit(timingReservoirSlidingTimeWindowDurationUnitName);
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
	 * @param timeUnit
	 * @return
	 */
	private String getPrettyName(final TimeUnit timeUnit) {
		return StringUtils.capitalize(timeUnit.toString());
	}

	/**
	 * 
	 */
	private void initializeMetrics() {
		LOGGER.debug(MARKER, "initializeMetrics()");

		// Exchange Rate
		String exchangeRateMetricName = MetricRegistry.name(this.name, "rate");
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
			String lclName = MetricRegistry.name(this.name, "interval" + getPrettyName(timeUnit));
			this.histogram = new Histogram(this.intervalReservoir);
			this.metricRegistry.register(lclName, this.histogram);
			this.intervals.put(timeUnit, this.histogram);
		}

		// Timing Metrics
		if (this.timingAction == TimingAction.START) {
			String lclName = MetricRegistry.name(this.name, this.timingName);
			LOGGER.debug(MARKER, "enabling timing metrics: {}", lclName);
			this.timer = new Timer(this.timingReservoir);
			this.metricRegistry.register(lclName, this.timer);
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
			this.histogram = new Histogram(this.histogramReservoir);
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

	/**
	 * 
	 */
	private void initializeReporters() {
		// jmx reporting
		if (this.isJmxReportingEnabled) {
			// @formatter:off
			this.jmxReporter = JmxReporter
					.forRegistry(this.metricRegistry)
					.inDomain(this.context)
					.convertDurationsTo(this.jmxDurationUnit)
					.convertRatesTo(this.jmxRateUnit)
					.build();
			// @formatter:on
		}
		// console reporting
		if (this.isConsoleReportingEnabled) {
			// @formatter:off
			this.consoleReporter = ConsoleReporter
					.forRegistry(this.metricRegistry)
					.convertDurationsTo(this.consoleDurationUnit)
					.convertRatesTo(this.consoleRateUnit)
					.build();
			// @formatter:on
			// reporter.start(1, TimeUnit.MINUTES);
		}
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
		if (this.jmxReporter != null) {
			this.jmxReporter.start();
		}
		if (this.consoleReporter != null) {
			this.consoleReporter.start(this.consoleReporterPeriod, this.consoleReporterPeriodUnit);
		}
	}

	@Override
	protected void doStop() throws Exception {
		super.doStop();
		LOGGER.debug(MARKER, "doStop()");
		if (this.jmxReporter != null) {
			this.jmxReporter.stop();
		}
		if (this.consoleReporter != null) {
			this.consoleReporter.stop();
		}
	}

	@Override
	protected void doSuspend() throws Exception {
		super.doSuspend();
		LOGGER.debug(MARKER, "doSuspend()");
	}

}
