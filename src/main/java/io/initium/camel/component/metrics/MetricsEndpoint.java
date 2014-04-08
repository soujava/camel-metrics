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

import java.net.InetSocketAddress;
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
import org.apache.camel.RuntimeCamelException;
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
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.SlidingTimeWindowReservoir;
import com.codahale.metrics.Timer;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

import io.initium.common.util.OptionHelper;
import io.initium.common.util.StringUtils;

import static io.initium.camel.component.metrics.MetricsComponent.DEFAULT_JMX_DOMAIN;
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

	// basic fields
	private final String					name;
	private final MetricsComponent			metricsComponent;
	private final MetricRegistry			metricRegistry;
	private String							jmxDomain										= DEFAULT_JMX_DOMAIN;

	// for default metrics
	private final Map<TimeUnit, Histogram>	intervals										= new HashMap<TimeUnit, Histogram>();
	private long							lastExchangeTime								= System.nanoTime();
	private Meter							exchangeRate									= null;
	private Timer							internalTimer									= null;
	private Exchange						lastExchange									= null;
	private boolean							isInternalTimerEnabled							= false;
	private Reservoir						intervalReservoir								= new ExponentiallyDecayingReservoir();

	// for timing metrics
	private Timer							timer											= null;
	private String							timingName										= "timing";
	private String							timingActionName								= null;
	private TimingAction					timingAction									= TimingAction.NOOP;
	private Reservoir						timingReservoir									= new ExponentiallyDecayingReservoir();
	private long							timingReservoirSlidingTimeWindowDuration		= 1;
	private TimeUnit						timingReservoirSlidingTimeWindowDurationUnit	= TimeUnit.HOURS;

	// for custom histogram metrics
	private Histogram						histogram										= null;
	private String							histogramName									= "histogram";
	private Expression						histogramValue									= null;
	private Reservoir						histogramReservoir								= new ExponentiallyDecayingReservoir();

	// for custom counter metrics
	private Counter							counter											= null;
	private String							counterName										= "count";
	private Expression						counterDelta									= null;

	// for custom gauge metrics
	private String							gaugeName										= "gauge";
	private Expression						gaugeValue										= null;
	private long							gaugeCacheDuration								= 10;
	private TimeUnit						gaugeCacheDurationUnit							= TimeUnit.SECONDS;

	// for jmx reporting
	private boolean							isJmxReportingEnabled							= true;
	private JmxReporter						jmxReporter										= null;
	private TimeUnit						jmxReporterDurationUnit							= TimeUnit.MILLISECONDS;
	private TimeUnit						jmxReporterRateUnit								= TimeUnit.SECONDS;

	// for console reporting
	private boolean							isConsoleReportingEnabled						= false;
	private ConsoleReporter					consoleReporter									= null;
	private TimeUnit						consoleReporterDurationUnit						= TimeUnit.MILLISECONDS;
	private TimeUnit						consoleReporterRateUnit							= TimeUnit.SECONDS;
	private long							consoleReporterPeriod							= 1;
	private TimeUnit						consoleReporterPeriodUnit						= TimeUnit.MINUTES;

	// for graphite reporting
	private boolean							isGraphiteReportingEnabled						= false;
	private GraphiteReporter				graphiteReporter								= null;
	private TimeUnit						graphiteReporterDurationUnit					= TimeUnit.MILLISECONDS;
	private TimeUnit						graphiteReporterRateUnit						= TimeUnit.SECONDS;
	private long							graphiteReporterPeriod							= 1;
	private TimeUnit						graphiteReporterPeriodUnit						= TimeUnit.MINUTES;
	private String							graphiteReporterHost							= "localhost";
	private int								graphiteReporterPort							= 2004;
	private String							graphiteReporterPrefix							= "prefix";

	/**
	 * @param uri
	 * @param metricsComponent
	 * @param name
	 * @param parameters
	 * @throws Exception
	 */
	public MetricsEndpoint(final String uri, final MetricsComponent metricsComponent, final String name, final Map<String, Object> parameters) throws Exception {
		super(uri, metricsComponent);
		LOGGER.debug(MARKER, "MetricsEndpoint({},{},{})", uri, metricsComponent, parameters);
		EndpointHelper.setProperties(getCamelContext(), this, parameters);
		this.name = name;
		this.metricsComponent = metricsComponent;
		this.metricsComponent.validateName(this.name);
		this.metricRegistry = metricsComponent.getMetricRegistry();
		switch (this.timingAction) {
			case STOP:
				LOGGER.debug(MARKER, "skipping initialization, timingAction={}", this.timingAction);
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
		throw new RuntimeCamelException("Cannot consume from a MetricsEndpoint: " + getEndpointUri());
	}

	@Override
	public Producer createProducer() throws Exception {
		LOGGER.debug(MARKER, "createProducer({})");
		final MetricsProducer producer = new MetricsProducer(this);
		return producer;
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
	};

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
	 * @return the context
	 */
	public String getJmxDomain() {
		return this.jmxDomain;
	}

	/**
	 * @return the jmxDurationUnit
	 */
	public TimeUnit getJmxDurationUnit() {
		return this.jmxReporterDurationUnit;
	}

	/**
	 * @return the jmxRateUnit
	 */
	public TimeUnit getJmxRateUnit() {
		return this.jmxReporterRateUnit;
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
		return this.timingActionName;
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
	 * @param consoleReporterDurationUnitName
	 *            the durationUnitName to set
	 */
	public void setConsoleDurationUnit(final String consoleReporterDurationUnitName) {
		this.consoleReporterDurationUnit = OptionHelper.parse(consoleReporterDurationUnitName, TimeUnit.class);
	}

	/**
	 * @param consoleReporterRateUnitName
	 *            the rateUnitName to set
	 */
	public void setConsoleRateUnit(final String consoleReporterRateUnitName) {
		this.consoleReporterRateUnit = OptionHelper.parse(consoleReporterRateUnitName, TimeUnit.class);
	}

	/**
	 * @param consoleReporterPeriod
	 *            the gaugeCacheDuration to set
	 */
	public void setConsoleReporterPeriod(final String consoleReporterPeriod) {
		long duration = Long.parseLong(consoleReporterPeriod);
		this.consoleReporterPeriod = duration;
	}

	/**
	 * @param consoleReporterPeriodUnitName
	 *            the rateUnitName to set
	 */
	public void setConsoleReporterPeriodUnit(final String consoleReporterPeriodUnitName) {
		this.consoleReporterPeriodUnit = OptionHelper.parse(consoleReporterPeriodUnitName, TimeUnit.class);
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
	 * @param enableConsoleReporting
	 */
	public void setEnableConsoleReporting(final String enableConsoleReporting) {
		this.isConsoleReportingEnabled = OptionHelper.parse(enableConsoleReporting, Boolean.class);
	}

	/**
	 * @param enableGraphiteReporting
	 */
	public void setEnableGraphiteReporting(final String enableGraphiteReporting) {
		this.isGraphiteReportingEnabled = OptionHelper.parse(enableGraphiteReporting, Boolean.class);
	}

	/**
	 * @param internalTimerEnabled
	 *            the internalTimerEnabled to set
	 */
	public void setEnableInternalTimer(final String internalTimerEnabled) {
		this.isInternalTimerEnabled = OptionHelper.parse(internalTimerEnabled, Boolean.class);
	}

	/**
	 * @param enableJmxReporting
	 */
	public void setEnableJmxReporting(final String enableJmxReporting) {
		this.isJmxReportingEnabled = OptionHelper.parse(enableJmxReporting, Boolean.class);
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
		this.gaugeCacheDurationUnit = OptionHelper.parse(gaugeCacheDurationUnitName, TimeUnit.class);
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
	 * @param graphiteReporterDurationUnitName
	 *            the graphiteReporterDurationUnit to set
	 */
	public void setGraphiteReporterDurationUnit(final String graphiteReporterDurationUnitName) {
		this.graphiteReporterDurationUnit = OptionHelper.parse(graphiteReporterDurationUnitName, TimeUnit.class);
	}

	/**
	 * @param graphiteReporterHost
	 *            the graphiteReporterHost to set
	 */
	public void setGraphiteReporterHost(final String graphiteReporterHost) {
		this.graphiteReporterHost = graphiteReporterHost;
	}

	/**
	 * @param graphiteReporterPeriod
	 *            the graphiteReporterPeriod to set
	 */
	public void setGraphiteReporterPeriod(final String graphiteReporterPeriod) {
		long duration = Long.parseLong(graphiteReporterPeriod);
		this.graphiteReporterPeriod = duration;

	}

	/**
	 * @param graphiteReporterPeriodUnitName
	 *            the graphiteReporterPeriodUnit to set
	 */
	public void setGraphiteReporterPeriodUnit(final String graphiteReporterPeriodUnitName) {
		this.graphiteReporterPeriodUnit = OptionHelper.parse(graphiteReporterPeriodUnitName, TimeUnit.class);
	}

	/**
	 * @param graphiteReporterPort
	 *            the graphiteReporterPort to set
	 */
	public void setGraphiteReporterPort(final String graphiteReporterPort) {
		int duration = Integer.parseInt(graphiteReporterPort);
		this.graphiteReporterPort = duration;
	}

	/**
	 * @param graphiteReporterPrefix
	 *            the graphiteReporterPrefix to set
	 */
	public void setGraphiteReporterPrefix(final String graphiteReporterPrefix) {
		this.graphiteReporterPrefix = graphiteReporterPrefix;
	}

	/**
	 * @param graphiteReporterRateUnitName
	 *            the graphiteReporterRateUnit to set
	 */
	public void setGraphiteReporterRateUnit(final String graphiteReporterRateUnitName) {
		this.graphiteReporterRateUnit = OptionHelper.parse(graphiteReporterRateUnitName, TimeUnit.class);
	}

	/**
	 * @param histogramName
	 *            the histogramName to set
	 */
	public void setHistogramName(final String histogramName) {
		this.histogramName = histogramName;
	}

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

	/**
	 * @param jmxDomain
	 *            the jmxDomain to set
	 */
	public void setJmxDomain(final String jmxDomain) {
		this.jmxDomain = jmxDomain;
	}

	/**
	 * @param jmxReporterDurationUnitName
	 *            the durationUnitName to set
	 */
	public void setJmxDurationUnit(final String jmxReporterDurationUnitName) {
		this.jmxReporterDurationUnit = OptionHelper.parse(jmxReporterDurationUnitName, TimeUnit.class);
	}

	/**
	 * @param jmxReporterRateUnitName
	 *            the rateUnitName to set
	 */
	public void setJmxRateUnit(final String jmxReporterRateUnitName) {
		this.jmxReporterRateUnit = OptionHelper.parse(jmxReporterRateUnitName, TimeUnit.class);
	}

	/**
	 * @param lastExchange
	 */
	public void setLastExchange(final Exchange lastExchange) {
		this.lastExchange = lastExchange;
	}

	/**
	 * @param timingActionName
	 *            the timing to set
	 */
	public void setTiming(final String timingActionName) {
		this.timingActionName = timingActionName;
		this.timingAction = TimingAction.valueOf(timingActionName.toUpperCase());
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
		this.timingReservoirSlidingTimeWindowDurationUnit = OptionHelper.parse(timingReservoirSlidingTimeWindowDurationUnitName, TimeUnit.class);
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
					.inDomain(this.jmxDomain)
					.convertDurationsTo(this.jmxReporterDurationUnit)
					.convertRatesTo(this.jmxReporterRateUnit)
					.build();
			// @formatter:on
		}
		// console reporting
		if (this.isConsoleReportingEnabled) {
			// @formatter:off
			this.consoleReporter = ConsoleReporter
					.forRegistry(this.metricRegistry)
					.convertDurationsTo(this.consoleReporterDurationUnit)
					.convertRatesTo(this.consoleReporterRateUnit)
					.build();
			// @formatter:on
		}
		// graphite reporting
		if (this.isGraphiteReportingEnabled) {
			final Graphite graphite = new Graphite(new InetSocketAddress(this.graphiteReporterHost, this.graphiteReporterPort));
			// @formatter:off
			this.graphiteReporter = GraphiteReporter
					.forRegistry(this.metricRegistry)
					.prefixedWith(this.graphiteReporterPrefix)
					.convertDurationsTo(this.graphiteReporterDurationUnit)
					.convertRatesTo(this.graphiteReporterRateUnit)
					.filter(MetricFilter.ALL)
					.build(graphite);
			// @formatter:off
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
		if (this.graphiteReporter != null) {
			this.graphiteReporter.start(this.graphiteReporterPeriod, this.graphiteReporterPeriodUnit);
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
		if (this.graphiteReporter != null) {
			this.graphiteReporter.stop();
		}	
	}

	@Override
	protected void doSuspend() throws Exception {
		super.doSuspend();
		LOGGER.debug(MARKER, "doSuspend()");
	}

}
