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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
import com.codahale.metrics.CsvReporter;
import com.codahale.metrics.ExponentiallyDecayingReservoir;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reservoir;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.initium.camel.component.metrics.reporters.ConsoleReporterDefinition;
import io.initium.camel.component.metrics.reporters.CsvReporterDefinition;
import io.initium.camel.component.metrics.reporters.GraphiteReporterDefinition;
import io.initium.camel.component.metrics.reporters.JmxReporterDefinition;
import io.initium.camel.component.metrics.reporters.ReporterDefinition;
import io.initium.camel.component.metrics.reporters.Slf4jReporterDefinition;
import io.initium.common.util.OptionHelper;
import io.initium.common.util.StringUtils;

import static io.initium.camel.component.metrics.MetricsComponent.MARKER;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.0
 * @since 2014-02-19
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class MetricsEndpoint extends DefaultEndpoint {

	// TODO remove suppress "rawtypes", "unchecked" warnings by refactoring ReporterDefinition

	/**
	 * 
	 */
	public enum TimingAction {
		NOOP, // no operation
		START, // start timers
		STOP; // stop timers
	}

	// logging
	private static final String				SELF					= Thread.currentThread().getStackTrace()[1].getClassName();
	private static final Logger				LOGGER					= LoggerFactory.getLogger(SELF);

	// basic fields
	private final String					name;
	private final MetricsComponent			metricsComponent;
	private final MetricRegistry			metricRegistry;

	// for default metrics
	private final Map<TimeUnit, Histogram>	intervals				= new HashMap<TimeUnit, Histogram>();
	private long							lastExchangeTime		= System.nanoTime();
	private Meter							exchangeRate			= null;
	private Timer							internalTimer			= null;
	private Exchange						lastExchange			= null;
	private boolean							isInternalTimerEnabled	= false;
	private Reservoir						intervalReservoir		= new ExponentiallyDecayingReservoir();

	// for timing metrics
	private Timer							timer					= null;
	private String							timingName				= "timing";
	private String							timingActionName		= null;
	private TimingAction					timingAction			= TimingAction.NOOP;
	private Reservoir						timingReservoir			= new ExponentiallyDecayingReservoir();

	// for custom histogram metrics
	private Histogram						histogram				= null;
	private String							histogramName			= "histogram";
	private Expression						histogramValue			= null;
	private Reservoir						histogramReservoir		= new ExponentiallyDecayingReservoir();

	// for custom counter metrics
	private Counter							counter					= null;
	private String							counterName				= "count";
	private Expression						counterDelta			= null;

	// for custom gauge metrics
	private String							gaugeName				= "gauge";
	private Expression						gaugeValue				= null;
	private long							gaugeCacheDuration		= 10;
	private TimeUnit						gaugeCacheDurationUnit	= TimeUnit.SECONDS;

	// for reporters
	private static final Gson				GSON					= new Gson();
	private static final Type				JMX_REPORTERS_TYPE		= new TypeToken<Collection<JmxReporterDefinition>>() {}.getType();
	private static final Type				CONSOLE_REPORTERS_TYPE	= new TypeToken<Collection<ConsoleReporterDefinition>>() {}.getType();
	private static final Type				GRAPHITE_REPORTERS_TYPE	= new TypeToken<Collection<GraphiteReporterDefinition>>() {}.getType();
	private static final Type				SLF4J_REPORTERS_TYPE	= new TypeToken<Collection<Slf4jReporterDefinition>>() {}.getType();
	private static final Type				CSV_REPORTERS_TYPE		= new TypeToken<Collection<CsvReporterDefinition>>() {}.getType();
	private final List<ReporterDefinition>	reporterDefinitions		= new ArrayList<ReporterDefinition>();
	private final List<JmxReporter>			jmxReporters			= new ArrayList<JmxReporter>();
	private final List<ConsoleReporter>		consoleReporters		= new ArrayList<ConsoleReporter>();
	private final List<GraphiteReporter>	graphiteReporters		= new ArrayList<GraphiteReporter>();
	private final List<Slf4jReporter>		slf4jReporters			= new ArrayList<Slf4jReporter>();
	private final List<CsvReporter>			csvReporters			= new ArrayList<CsvReporter>();

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
		this.metricsComponent = metricsComponent;
		this.name = name;
		this.metricsComponent.registerName(this.name);
		// this.metricRegistry = metricsComponent.getMetricRegistry();
		this.metricRegistry = new MetricRegistry();
		warnIfTimingStopIsUsedWithOtherParameters(parameters);
		EndpointHelper.setProperties(getCamelContext(), this, parameters);
		switch (this.timingAction) {
			case STOP:
				LOGGER.debug(MARKER, "skipping initialization, timingAction={}", this.timingAction);
				break;
			default:
				initializeMetrics();
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
	}

	/**
	 * @return the exchangeRateMetric
	 */
	public Meter getExchangeRate() {
		return this.exchangeRate;
	};

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
	 * @param consoleReporters
	 *            the consoleReporters to set
	 */
	public void setConsoleReporters(final String consoleReporters) {
		List<ConsoleReporterDefinition> consoleReporterDefinitions = GSON.fromJson(consoleReporters, CONSOLE_REPORTERS_TYPE);
		for (ConsoleReporterDefinition consoleReporterDefinition : consoleReporterDefinitions) {
			this.reporterDefinitions.add(consoleReporterDefinition);
		}
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
	 * @param csvReporters
	 *            the csvReporters to set
	 */
	public void setCsvReporters(final String csvReporters) {
		List<CsvReporterDefinition> csvReporterDefinitions = GSON.fromJson(csvReporters, CSV_REPORTERS_TYPE);
		for (CsvReporterDefinition csvReporterDefinition : csvReporterDefinitions) {
			this.reporterDefinitions.add(csvReporterDefinition);
		}
	}

	/**
	 * @param internalTimerEnabled
	 *            the internalTimerEnabled to set
	 */
	public void setEnableInternalTimer(final String internalTimerEnabled) {
		this.isInternalTimerEnabled = OptionHelper.parse(internalTimerEnabled, Boolean.class);
	}

	/**
	 * @param gaugeCacheDuration
	 *            the gaugeCacheDuration to set
	 */
	public void setGaugeCacheDuration(final long gaugeCacheDuration) {
		this.gaugeCacheDuration = gaugeCacheDuration;
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
	 * @param graphiteReporters
	 *            the graphiteReporters to set
	 */
	public void setGraphiteReporters(final String graphiteReporters) {
		List<GraphiteReporterDefinition> graphiteReporterDefinitions = GSON.fromJson(graphiteReporters, GRAPHITE_REPORTERS_TYPE);
		for (GraphiteReporterDefinition graphiteReporterDefinition : graphiteReporterDefinitions) {
			this.reporterDefinitions.add(graphiteReporterDefinition);
		}
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
	 * @param jmxReporters
	 *            the jmxReporters to set
	 */
	public void setJmxReporters(final String jmxReporters) {
		List<JmxReporterDefinition> jmxReporterDefinitions = GSON.fromJson(jmxReporters, JMX_REPORTERS_TYPE);
		for (JmxReporterDefinition jmxReporterDefinition : jmxReporterDefinitions) {
			this.reporterDefinitions.add(jmxReporterDefinition);
		}
	}

	/**
	 * @param lastExchange
	 */
	public void setLastExchange(final Exchange lastExchange) {
		this.lastExchange = lastExchange;
	}

	/**
	 * @param slf4jReporters
	 *            the slf4jReporters to set
	 */
	public void setSlf4jReporters(final String slf4jReporters) {
		List<Slf4jReporterDefinition> slf4jReporterDefinitions = GSON.fromJson(slf4jReporters, SLF4J_REPORTERS_TYPE);
		for (Slf4jReporterDefinition slf4jReporterDefinition : slf4jReporterDefinitions) {
			this.reporterDefinitions.add(slf4jReporterDefinition);
		}
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
				String localName = timingReservoirName.substring(1);
				if (localName.length() > 0) {
					this.timingReservoir = CamelContextHelper.mandatoryLookup(getCamelContext(), timingReservoirName, Reservoir.class);
				}
			}
			if (this.timingReservoir == null) {
				throw new NoSuchBeanException(this.name);
			}
		}
	}

	/**
	 * @param expression
	 * @return
	 */
	private Expression createFileLanguageExpression(final String expression) {
		Language language;
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
	 * @param reporterDefinition
	 */
	private void registerAndStart(final ReporterDefinition reporterDefinition) {
		if (reporterDefinition instanceof JmxReporterDefinition) {
			JmxReporterDefinition jmxReporterDefinition = ((JmxReporterDefinition) reporterDefinition).getReporterDefinitionWithDefaults();
			LOGGER.info(MARKER, "adding JmxReporterDefinition: {}", jmxReporterDefinition);
			JmxReporter jmxReporter = jmxReporterDefinition.buildReporter(this.metricRegistry);
			this.jmxReporters.add(jmxReporter);
			LOGGER.info(MARKER, "starting reporter: {}", jmxReporter);
			jmxReporter.start();
		} else if (reporterDefinition instanceof ConsoleReporterDefinition) {
			ConsoleReporterDefinition consoleReporterDefinition = ((ConsoleReporterDefinition) reporterDefinition).getReporterDefinitionWithDefaults();
			LOGGER.info(MARKER, "adding ConsoleReporterDefinition: {}", consoleReporterDefinition);
			ConsoleReporter consoleReporter = consoleReporterDefinition.buildReporter(this.metricRegistry);
			this.consoleReporters.add(consoleReporter);
			LOGGER.info(MARKER, "starting reporter: {}", consoleReporter);
			consoleReporter.start(consoleReporterDefinition.getPeriodDuration(), consoleReporterDefinition.getPeriodDurationUnit());
		} else if (reporterDefinition instanceof GraphiteReporterDefinition) {
			GraphiteReporterDefinition graphiteReporterDefinition = ((GraphiteReporterDefinition) reporterDefinition).getReporterDefinitionWithDefaults();
			LOGGER.info(MARKER, "adding GraphiteReporterDefinition: {}", graphiteReporterDefinition);
			GraphiteReporter graphiteReporter = graphiteReporterDefinition.buildReporter(this.metricRegistry);
			this.graphiteReporters.add(graphiteReporter);
			LOGGER.info(MARKER, "starting reporter: {}", graphiteReporter);
			graphiteReporter.start(graphiteReporterDefinition.getPeriodDuration(), graphiteReporterDefinition.getPeriodDurationUnit());
		} else if (reporterDefinition instanceof Slf4jReporterDefinition) {
			Slf4jReporterDefinition slf4jReporterDefinition = ((Slf4jReporterDefinition) reporterDefinition).getReporterDefinitionWithDefaults();
			LOGGER.info(MARKER, "adding Slf4jReporterDefinition: {}", slf4jReporterDefinition);
			Slf4jReporter slf4jReporter = slf4jReporterDefinition.buildReporter(this.metricRegistry);
			this.slf4jReporters.add(slf4jReporter);
			LOGGER.info(MARKER, "starting reporter: {}", slf4jReporter);
			slf4jReporter.start(slf4jReporterDefinition.getPeriodDuration(), slf4jReporterDefinition.getPeriodDurationUnit());
		} else if (reporterDefinition instanceof CsvReporterDefinition) {
			CsvReporterDefinition csvReporterDefinition = ((CsvReporterDefinition) reporterDefinition).getReporterDefinitionWithDefaults();
			LOGGER.info(MARKER, "adding CsvjReporterDefinition: {}", csvReporterDefinition);
			CsvReporter csvReporter = csvReporterDefinition.buildReporter(this.metricRegistry);
			this.csvReporters.add(csvReporter);
			LOGGER.info(MARKER, "starting reporter: {}", csvReporter);
			csvReporter.start(csvReporterDefinition.getPeriodDuration(), csvReporterDefinition.getPeriodDurationUnit());
		} else {
			LOGGER.warn(MARKER, "unsupported ReporterDefinition: {}: {}", reporterDefinition.getClass(), reporterDefinition);
		}
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

	private void warnIfTimingStopIsUsedWithOtherParameters(final Map<String, Object> parameters) {
		if (parameters.containsKey("timing")) {
			Object value = parameters.get("timing");
			if (value instanceof String) {
				String stringValue = (String) value;
				if (TimingAction.STOP.name().equalsIgnoreCase(stringValue) && parameters.size() > 1) {
					LOGGER.warn(MARKER, "found timing={}, additional parameters may be ignored: {}", stringValue, parameters);
				}
			}
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
		LOGGER.debug(MARKER, "doStart()");

		Map<String, ReporterDefinition> componentReporterDefinitions = this.metricsComponent.getReporterDefinitions();
		Map<String, ReporterDefinition> leftoverReporterDefinitions = new HashMap<String, ReporterDefinition>();
		leftoverReporterDefinitions.putAll(componentReporterDefinitions);

		// check component definitions for defaults
		for (ReporterDefinition reporterDefinition : this.reporterDefinitions) {
			String reporterDefinitionName = reporterDefinition.getName();
			ReporterDefinition componentReporterDefinition = leftoverReporterDefinitions.get(reporterDefinitionName);
			if (componentReporterDefinition != null) {
				ReporterDefinition combinedReporterDefinition = componentReporterDefinition;
				combinedReporterDefinition = componentReporterDefinition.applyAsOverride(reporterDefinition);
				registerAndStart(combinedReporterDefinition);
				leftoverReporterDefinitions.remove(reporterDefinitionName);
			} else {
				registerAndStart(reporterDefinition);
			}
		}
		// start the remaining definitions
		for (Entry<String, ReporterDefinition> leftoverReporterDefinitionEntry : leftoverReporterDefinitions.entrySet()) {
			ReporterDefinition reporterDefinition = leftoverReporterDefinitionEntry.getValue();
			registerAndStart(reporterDefinition);
		}
	}

	@Override
	protected void doStop() throws Exception {
		LOGGER.debug(MARKER, "doStop()");
		for (JmxReporter jmxReporter : this.jmxReporters) {
			jmxReporter.stop();
		}
		for (ConsoleReporter consoleReporter : this.consoleReporters) {
			consoleReporter.stop();
		}
		for (GraphiteReporter graphiteReporter : this.graphiteReporters) {
			graphiteReporter.stop();
		}
		for (Slf4jReporter slf4jReporter : this.slf4jReporters) {
			slf4jReporter.stop();
		}
		for (CsvReporter csvReporter : this.csvReporters) {
			csvReporter.stop();
		}
	}

	@Override
	protected void doSuspend() throws Exception {
		super.doSuspend();
		LOGGER.debug(MARKER, "doSuspend()");
	}

}
