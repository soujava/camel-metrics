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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.MultipleConsumersSupport;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.util.EndpointHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;

import io.initium.camel.component.metrics.definition.metric.CachedGaugeDefinition;
import io.initium.camel.component.metrics.definition.metric.CounterDefinition;
import io.initium.camel.component.metrics.definition.metric.GaugeDefinition;
import io.initium.camel.component.metrics.definition.metric.HistogramDefinition;
import io.initium.camel.component.metrics.definition.metric.MeterDefinition;
import io.initium.camel.component.metrics.definition.metric.TimerDefinition;
import io.initium.camel.component.metrics.definition.reporter.ConsoleReporterDefinition;
import io.initium.camel.component.metrics.definition.reporter.CsvReporterDefinition;
import io.initium.camel.component.metrics.definition.reporter.GraphiteReporterDefinition;
import io.initium.camel.component.metrics.definition.reporter.JmxReporterDefinition;
import io.initium.camel.component.metrics.definition.reporter.ReporterDefinition;
import io.initium.camel.component.metrics.definition.reporter.Slf4jReporterDefinition;
import io.initium.common.util.ExpressionUtils;
import io.initium.common.util.MetricUtils;
import io.initium.common.util.OptionHelper;

import static io.initium.camel.component.metrics.MetricsComponent.MARKER;
import static io.initium.common.util.ExpressionUtils.createExpression;
import static io.initium.common.util.GsonHelper.CACHED_GAUGE_DEFINITIONS_TYPE;
import static io.initium.common.util.GsonHelper.CACHED_GAUGE_DEFINITION_TYPE;
import static io.initium.common.util.GsonHelper.CONSOLE_REPORTERS_TYPE;
import static io.initium.common.util.GsonHelper.CONSOLE_REPORTER_TYPE;
import static io.initium.common.util.GsonHelper.COUNTER_DEFINITIONS_TYPE;
import static io.initium.common.util.GsonHelper.COUNTER_DEFINITION_TYPE;
import static io.initium.common.util.GsonHelper.CSV_REPORTERS_TYPE;
import static io.initium.common.util.GsonHelper.CSV_REPORTER_TYPE;
import static io.initium.common.util.GsonHelper.GAUGE_DEFINITIONS_TYPE;
import static io.initium.common.util.GsonHelper.GAUGE_DEFINITION_TYPE;
import static io.initium.common.util.GsonHelper.GRAPHITE_REPORTERS_TYPE;
import static io.initium.common.util.GsonHelper.GRAPHITE_REPORTER_TYPE;
import static io.initium.common.util.GsonHelper.GSON;
import static io.initium.common.util.GsonHelper.HISTOGRAM_DEFINITIONS_TYPE;
import static io.initium.common.util.GsonHelper.HISTOGRAM_DEFINITION_TYPE;
import static io.initium.common.util.GsonHelper.INFIXES_TYPE;
import static io.initium.common.util.GsonHelper.INFIX_TYPE;
import static io.initium.common.util.GsonHelper.JMX_REPORTERS_TYPE;
import static io.initium.common.util.GsonHelper.JMX_REPORTER_TYPE;
import static io.initium.common.util.GsonHelper.METER_DEFINITIONS_TYPE;
import static io.initium.common.util.GsonHelper.METER_DEFINITION_TYPE;
import static io.initium.common.util.GsonHelper.SLF4J_REPORTERS_TYPE;
import static io.initium.common.util.GsonHelper.SLF4J_REPORTER_TYPE;
import static io.initium.common.util.GsonHelper.TIME_UNITS_TYPE;
import static io.initium.common.util.GsonHelper.TIME_UNIT_TYPE;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.0
 * @since 2014-02-19
 */
@SuppressWarnings({"rawtypes"})
@ManagedResource(description = "Managed MetricsEndpoint")
public class MetricsEndpoint extends DefaultEndpoint implements MultipleConsumersSupport {

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
	private List<Expression>				infixExpressions;

	// for internal timer
	private boolean							isInternalTimerEnabled	= false;
	private final Timer						internalTimerStart		= null;
	private final Timer						internalTimerNoop		= null;
	private final Timer						internalTimerStop		= null;

	// base metric names
	private String							rateName				= "rate";
	private String							sinceName				= "since";
	private String							intervalName			= "interval";
	private String							timingName				= "timing";

	// for timer metric
	private final Timer						timer					= null;
	private String							timingActionName		= null;

	private TimingAction					timingAction			= TimingAction.NOOP;
	// for expression based metrics
	private List<HistogramDefinition>		histogramDefinitions;
	private List<CounterDefinition>			counterDefinitions;
	private List<MeterDefinition>			meterDefinitions;
	private List<GaugeDefinition>			gaugeDefinitions;
	private List<CachedGaugeDefinition>		cachedGaugeDefinitions;

	// for reporters
	private final List<ReporterDefinition>	reporterDefinitions		= new ArrayList<ReporterDefinition>();
	private List<TimeUnit>					sinceTimeUnits;
	private List<TimeUnit>					intervalTimeUnits;

	/**
	 * @param uri
	 * @param metricsComponent
	 * @param name
	 * @param parameters
	 * @throws Exception
	 */
	public MetricsEndpoint(final String uri, final MetricsComponent metricsComponent, final String name, final Map<String, Object> parameters) throws Exception {
		super(uri, metricsComponent);
		LOGGER.info(MARKER, "MetricsEndpoint({},{},{})", uri, metricsComponent, parameters);
		this.metricsComponent = metricsComponent;
		this.name = name;
		warnIfTimingStopIsUsedWithOtherParameters(parameters);
		EndpointHelper.setProperties(getCamelContext(), this, parameters);
		switch (this.timingAction) {
			case STOP:
				LOGGER.debug(MARKER, "skipping initialization, timingAction={}", this.timingAction);
				break;
			default:
				initializeMetricGroup(this.name);
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
	 * @return the infixExpressions
	 */
	public List<Expression> getInfixExpressions() {
		return this.infixExpressions;
	}

	/**
	 * @return
	 */
	public Timer getInternalTimer() {
		return getInternalTimer(TimingAction.NOOP);
	}

	/**
	 * @param timingAction
	 * @return
	 */
	public Timer getInternalTimer(final TimingAction timingAction) {
		switch (timingAction) {
			case START:
				return this.internalTimerStart;
			case STOP:
				return this.internalTimerStop;
			default:
				return this.internalTimerNoop;
		}
	}

	/**
	 * @return the intervalName
	 */
	public String getIntervalName() {
		return this.intervalName;
	}

	/**
	 * @return the intervalTimeUnits
	 */
	public List<TimeUnit> getIntervalTimeUnits() {
		return this.intervalTimeUnits;
	}

	/**
	 * @return
	 */
	public MetricsComponent getMetricComponent() {
		return this.metricsComponent;
	}

	public MetricRegistry getMetricsRegistry() {
		return this.metricsComponent.getMetricRegistry();
	}

	/**
	 * @return
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return the rateName
	 */
	public String getRateName() {
		return this.rateName;
	}

	/**
	 * @return the reporterDefinitions
	 */
	public List<ReporterDefinition> getReporterDefinitions() {
		return this.reporterDefinitions;
	}

	/**
	 * @return the sinceName
	 */
	public String getSinceName() {
		return this.sinceName;
	}

	/**
	 * @return the sinceTimeUnits
	 */
	public List<TimeUnit> getSinceTimeUnits() {
		return this.sinceTimeUnits;
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
	 * @param baseName
	 * @param infixName
	 * @return
	 * @throws Exception
	 */
	public synchronized MetricGroup initializeMetricGroup(final String baseName, final String infixName) throws Exception {
		LOGGER.debug(MARKER, "initializeMetricGroup({},{})", baseName, infixName);
		String fullMetricGroupName = MetricUtils.calculateFullMetricName(baseName, infixName);
		MetricGroup metricGroup = this.metricsComponent.getMetricGroups().get(fullMetricGroupName);
		if (metricGroup != null) {
			return metricGroup;
		}
		this.metricsComponent.registerName(this.name);
		metricGroup = new MetricGroup(this, baseName, infixName);
		// timer
		if (this.timingAction == TimingAction.START) {
			TimerDefinition timerDefinition = new TimerDefinition();
			timerDefinition.setName(this.timingName);
			metricGroup.addTimerDefinition(timerDefinition);
		}

		// expression based histograms
		metricGroup.addCounterDefinitions(this.counterDefinitions);
		metricGroup.addMeterDefinitions(this.meterDefinitions);
		metricGroup.addHistogramDefinitions(this.histogramDefinitions);
		metricGroup.addGaugeDefinitions(this.gaugeDefinitions);
		metricGroup.addCachedGaugeDefinitions(this.cachedGaugeDefinitions);

		this.metricsComponent.getMetricGroups().put(fullMetricGroupName, metricGroup);
		getCamelContext().addService(metricGroup);
		return metricGroup;
	}

	/**
	 * @param baseName
	 * @param infixName
	 * @param creatingExchange
	 * @return
	 * @throws Exception
	 */
	public synchronized MetricGroup initializeMetricGroup(final String baseName, final String infixName, final Exchange creatingExchange) throws Exception {
		LOGGER.debug(MARKER, "initializeMetricGroup({},{})", baseName, infixName);
		String fullMetricGroupName = MetricUtils.calculateFullMetricName(baseName, infixName);
		MetricGroup metricGroup = this.metricsComponent.getMetricGroups().get(fullMetricGroupName);
		if (metricGroup != null) {
			return metricGroup;
		}
		metricGroup = new MetricGroup(this, baseName, infixName, creatingExchange);
		// timer
		if (this.timingAction == TimingAction.START) {
			TimerDefinition timerDefinition = new TimerDefinition();
			timerDefinition.setName(this.timingName);
			metricGroup.addTimerDefinition(timerDefinition);
		}

		// expression based histograms
		metricGroup.addCounterDefinitions(this.counterDefinitions);
		metricGroup.addMeterDefinitions(this.meterDefinitions);
		metricGroup.addHistogramDefinitions(this.histogramDefinitions);
		metricGroup.addGaugeDefinitions(this.gaugeDefinitions);
		metricGroup.addCachedGaugeDefinitions(this.cachedGaugeDefinitions);

		//
		this.metricsComponent.getMetricGroups().put(fullMetricGroupName, metricGroup);
		getCamelContext().addService(metricGroup);
		return metricGroup;
	}

	/**
	 * @return the isInternalTimerEnabled
	 */
	public boolean isInternalTimerEnabled() {
		return this.isInternalTimerEnabled;
	}

	@Override
	public boolean isMultipleConsumersSupported() {
		return true;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	/**
	 * @param baseName
	 * @return
	 * @throws Exception
	 */
	public MetricGroup lookupMetricGroup(final String baseName) throws Exception {
		return lookupMetricGroup(baseName, null);
	}

	/**
	 * @param baseName
	 * @param infixName
	 * @return
	 * @throws Exception
	 */
	public MetricGroup lookupMetricGroup(final String baseName, final String infixName) throws Exception {
		String fullMetricGroupName = MetricUtils.calculateFullMetricName(baseName, infixName);
		MetricGroup metricGroup = this.metricsComponent.getMetricGroups().get(fullMetricGroupName);
		if (metricGroup != null) {
			return metricGroup;
		}
		return initializeMetricGroup(baseName, infixName);
	}

	/**
	 * @param baseName
	 * @param infixName
	 * @return
	 * @throws Exception
	 */
	public MetricGroup lookupMetricGroup(final String baseName, final String infixName, final Exchange exchange) throws Exception {
		String fullMetricGroupName = MetricUtils.calculateFullMetricName(baseName, infixName);
		MetricGroup metricGroup = this.metricsComponent.getMetricGroups().get(fullMetricGroupName);
		if (metricGroup != null) {
			return metricGroup;
		}
		return initializeMetricGroup(baseName, infixName, exchange);
	}

	/**
	 * @param cachedGauges
	 *            the cachedGauges to set
	 */
	public void setCachedGauge(final String cachedGauges) {
		setCachedGauges(cachedGauges);
	}

	/**
	 * @param cachedGauges
	 *            the cachedGauges to set
	 */
	public void setCachedGauges(final String cachedGauges) {
		List<CachedGaugeDefinition> cacheGaugeDefinitions;
		try {
			cacheGaugeDefinitions = GSON.fromJson(cachedGauges, CACHED_GAUGE_DEFINITIONS_TYPE);
		} catch (Exception e) {
			CachedGaugeDefinition cachedGaugeDefinition = GSON.fromJson(cachedGauges, CACHED_GAUGE_DEFINITION_TYPE);
			cacheGaugeDefinitions = new ArrayList<CachedGaugeDefinition>();
			cacheGaugeDefinitions.add(cachedGaugeDefinition);
		}
		this.cachedGaugeDefinitions = cacheGaugeDefinitions;
		for (CachedGaugeDefinition cachedGaugeDefinition : cacheGaugeDefinitions) {
			cachedGaugeDefinition.setExpression(ExpressionUtils.createExpression(cachedGaugeDefinition.getValue(), getCamelContext()));
		}
	}

	/**
	 * @param consoleReporter
	 */
	public void setConsoleReporter(final String consoleReporter) {
		setConsoleReporters(consoleReporter);
	}

	/**
	 * @param consoleReporters
	 *            the consoleReporters to set
	 */
	public void setConsoleReporters(final String consoleReporters) {
		List<ConsoleReporterDefinition> consoleReporterDefinitions;
		try {
			consoleReporterDefinitions = GSON.fromJson(consoleReporters, CONSOLE_REPORTERS_TYPE);
		} catch (Exception e) {
			ConsoleReporterDefinition consoleReporterDefinition = GSON.fromJson(consoleReporters, CONSOLE_REPORTER_TYPE);
			consoleReporterDefinitions = new ArrayList<ConsoleReporterDefinition>();
			consoleReporterDefinitions.add(consoleReporterDefinition);
		}
		for (ConsoleReporterDefinition consoleReporterDefinition : consoleReporterDefinitions) {
			this.reporterDefinitions.add(consoleReporterDefinition);
		}
	}

	/**
	 * @param counter
	 *            the counter to set
	 */
	public void setCounter(final String counter) {
		setCounters(counter);
	}

	/**
	 * @param counters
	 *            the counters to set
	 */
	public void setCounters(final String counters) {
		List<CounterDefinition> counterDefinitions;
		try {
			counterDefinitions = GSON.fromJson(counters, COUNTER_DEFINITIONS_TYPE);
		} catch (Exception e) {
			CounterDefinition counterDefinition = GSON.fromJson(counters, COUNTER_DEFINITION_TYPE);
			counterDefinitions = new ArrayList<CounterDefinition>();
			counterDefinitions.add(counterDefinition);
		}
		for (CounterDefinition counterDefinition : counterDefinitions) {
			counterDefinition.setExpression(createExpression(counterDefinition.getValue(), getCamelContext()));
		}
		this.counterDefinitions = counterDefinitions;
	}

	/**
	 * @param csvReporter
	 */
	public void setCsvReporter(final String csvReporter) {
		setCsvReporters(csvReporter);
	}

	/**
	 * @param csvReporters
	 *            the csvReporters to set
	 */
	public void setCsvReporters(final String csvReporters) {
		List<CsvReporterDefinition> csvReporterDefinitions;
		try {
			csvReporterDefinitions = GSON.fromJson(csvReporters, CSV_REPORTERS_TYPE);
		} catch (Exception e) {
			CsvReporterDefinition csvReporterDefinition = GSON.fromJson(csvReporters, CSV_REPORTER_TYPE);
			csvReporterDefinitions = new ArrayList<CsvReporterDefinition>();
			csvReporterDefinitions.add(csvReporterDefinition);
		}
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
	 * @param gauge
	 *            the gauge to set
	 */
	public void setGauge(final String gauge) {
		setGauges(gauge);
	}

	/**
	 * @param gauges
	 *            the gauges to set
	 */
	public void setGauges(final String gauges) {
		List<GaugeDefinition> gaugeDefinitions;
		try {
			gaugeDefinitions = GSON.fromJson(gauges, GAUGE_DEFINITIONS_TYPE);
		} catch (Exception e) {
			GaugeDefinition gaugeDefinition = GSON.fromJson(gauges, GAUGE_DEFINITION_TYPE);
			gaugeDefinitions = new ArrayList<GaugeDefinition>();
			gaugeDefinitions.add(gaugeDefinition);
		}
		this.gaugeDefinitions = gaugeDefinitions;
		for (GaugeDefinition gaugeDefinition : this.gaugeDefinitions) {
			gaugeDefinition.setExpression(createExpression(gaugeDefinition.getValue(), getCamelContext()));
		}
	}

	/**
	 * @param graphiteReporter
	 */
	public void setGraphiteReporter(final String graphiteReporter) {
		setGraphiteReporters(graphiteReporter);
	}

	/**
	 * @param graphiteReporters
	 *            the graphiteReporters to set
	 */
	public void setGraphiteReporters(final String graphiteReporters) {
		List<GraphiteReporterDefinition> graphiteReporterDefinitions;
		try {
			graphiteReporterDefinitions = GSON.fromJson(graphiteReporters, GRAPHITE_REPORTERS_TYPE);
		} catch (Exception e) {
			GraphiteReporterDefinition graphiteReporterDefinition = GSON.fromJson(graphiteReporters, GRAPHITE_REPORTER_TYPE);
			graphiteReporterDefinitions = new ArrayList<GraphiteReporterDefinition>();
			graphiteReporterDefinitions.add(graphiteReporterDefinition);
		}
		for (GraphiteReporterDefinition graphiteReporterDefinition : graphiteReporterDefinitions) {
			this.reporterDefinitions.add(graphiteReporterDefinition);
		}
	}

	/**
	 * @param histogram
	 *            the histogram to set
	 */
	public void setHistogram(final String histogram) {
		setHistograms(histogram);
	}

	/**
	 * @param histograms
	 *            the histograms to set
	 */
	public void setHistograms(final String histograms) {
		List<HistogramDefinition> histogramDefinitions;
		try {
			histogramDefinitions = GSON.fromJson(histograms, HISTOGRAM_DEFINITIONS_TYPE);
		} catch (Exception e) {
			HistogramDefinition histogramDefinition = GSON.fromJson(histograms, HISTOGRAM_DEFINITION_TYPE);
			histogramDefinitions = new ArrayList<HistogramDefinition>();
			histogramDefinitions.add(histogramDefinition);
		}
		for (HistogramDefinition histogramDefinition : histogramDefinitions) {
			histogramDefinition.setExpression(createExpression(histogramDefinition.getValue(), getCamelContext()));
		}
		this.histogramDefinitions = histogramDefinitions;
	}

	/**
	 * @param infix
	 *            the infix to set
	 */
	public void setInfix(final String infix) {
		setInfixes(infix);
	}

	/**
	 * @param infixesJson
	 *            the gauges to set
	 */
	public void setInfixes(final String infixesJson) {
		// this.infixExpression = createFileLanguageExpression(infix);
		List<String> infixes;
		try {
			infixes = GSON.fromJson(infixesJson, INFIXES_TYPE);
		} catch (Exception e) {
			String infix = GSON.fromJson(infixesJson, INFIX_TYPE);
			infixes = new ArrayList<String>();
			infixes.add(infix);
		}
		this.infixExpressions = new ArrayList<Expression>();
		for (String infix : infixes) {
			this.infixExpressions.add(createExpression(infix, getCamelContext()));
		}
	}

	/**
	 * @param intervalName
	 *            the intervalName to set
	 */
	public void setIntervalName(final String intervalName) {
		this.intervalName = intervalName;
	}

	/**
	 * @param intervalTimeUnitString
	 *            the intervalTimeUnitString to set
	 */
	public void setIntervalTimeUnit(final String intervalTimeUnitString) {
		setIntervalTimeUnits(intervalTimeUnitString);
	}

	/**
	 * @param intervalTimeUnitsString
	 *            the intervalTimeUnitsString to set
	 */
	public void setIntervalTimeUnits(final String intervalTimeUnitsString) {
		List<TimeUnit> intervalTimeUnits;
		try {
			intervalTimeUnits = GSON.fromJson(intervalTimeUnitsString, TIME_UNITS_TYPE);
		} catch (Exception e) {
			TimeUnit intervalTimeUnit = GSON.fromJson(intervalTimeUnitsString, TIME_UNIT_TYPE);
			intervalTimeUnits = new ArrayList<TimeUnit>();
			intervalTimeUnits.add(intervalTimeUnit);
		}
		this.intervalTimeUnits = intervalTimeUnits;
	}

	/**
	 * @param jmxReporter
	 *            the jmxReporter to set
	 */
	public void setJmxReporter(final String jmxReporter) {
		setJmxReporters(jmxReporter);
	}

	/**
	 * @param jmxReporters
	 *            the jmxReporters to set
	 */
	public void setJmxReporters(final String jmxReporters) {
		List<JmxReporterDefinition> jmxReporterDefinitions;
		try {
			jmxReporterDefinitions = GSON.fromJson(jmxReporters, JMX_REPORTERS_TYPE);
		} catch (Exception e) {
			JmxReporterDefinition jmxReporterDefinition = GSON.fromJson(jmxReporters, JMX_REPORTER_TYPE);
			jmxReporterDefinitions = new ArrayList<JmxReporterDefinition>();
			jmxReporterDefinitions.add(jmxReporterDefinition);
		}
		for (JmxReporterDefinition jmxReporterDefinition : jmxReporterDefinitions) {
			this.reporterDefinitions.add(jmxReporterDefinition);
		}
	}

	/**
	 * @param meter
	 *            the meter to set
	 */
	public void setMeter(final String meter) {
		setMeters(meter);
	}

	// /**
	// * @param timingReservoirName
	// * the timingReservoir to set
	// */
	// public void setTimingReservoir(final String timingReservoirName) {
	// if (timingReservoirName != null && timingReservoirName.length() > 0) {
	// char firstChar = timingReservoirName.charAt(0);
	// if (firstChar == '#') {
	// String localName = timingReservoirName.substring(1);
	// if (localName.length() > 0) {
	// this.timingReservoir = CamelContextHelper.mandatoryLookup(getCamelContext(), timingReservoirName,
	// Reservoir.class);
	// }
	// }
	// if (this.timingReservoir == null) {
	// throw new NoSuchBeanException(this.name);
	// }
	// }
	// }

	/**
	 * @param meters
	 *            the meters to set
	 */
	public void setMeters(final String meters) {
		List<MeterDefinition> meterDefinitions;
		try {
			meterDefinitions = GSON.fromJson(meters, METER_DEFINITIONS_TYPE);
		} catch (Exception e) {
			MeterDefinition meterDefinition = GSON.fromJson(meters, METER_DEFINITION_TYPE);
			meterDefinitions = new ArrayList<MeterDefinition>();
			meterDefinitions.add(meterDefinition);
		}
		for (MeterDefinition meterDefinition : meterDefinitions) {
			meterDefinition.setExpression(createExpression(meterDefinition.getValue(), getCamelContext()));
		}
		this.meterDefinitions = meterDefinitions;
	}

	/**
	 * @param rateName
	 *            the rateName to set
	 */
	public void setRateName(final String rateName) {
		this.rateName = rateName;
	}

	/**
	 * @param sinceName
	 *            the sinceName to set
	 */
	public void setSinceName(final String sinceName) {
		this.sinceName = sinceName;
	}

	/**
	 * @param sinceTimeUnitString
	 *            the sinceTimeUnitString to set
	 */
	public void setSinceTimeUnit(final String sinceTimeUnitString) {
		setSinceTimeUnits(sinceTimeUnitString);
	}

	/**
	 * @param sinceTimeUnitsString
	 *            the sinceTimeUnitsString to set
	 */
	public void setSinceTimeUnits(final String sinceTimeUnitsString) {
		List<TimeUnit> sinceTimeUnits;
		try {
			sinceTimeUnits = GSON.fromJson(sinceTimeUnitsString, TIME_UNITS_TYPE);
		} catch (Exception e) {
			TimeUnit sinceTimeUnit = GSON.fromJson(sinceTimeUnitsString, TIME_UNIT_TYPE);
			sinceTimeUnits = new ArrayList<TimeUnit>();
			sinceTimeUnits.add(sinceTimeUnit);
		}
		this.sinceTimeUnits = sinceTimeUnits;
	}

	/**
	 * @param slf4jReporter
	 *            the slf4jReporter to set
	 */
	public void setSlf4jReporter(final String slf4jReporter) {
		setSlf4jReporters(slf4jReporter);
	}

	/**
	 * @param slf4jReporters
	 *            the slf4jReporters to set
	 */
	public void setSlf4jReporters(final String slf4jReporters) {
		List<Slf4jReporterDefinition> slf4jReporterDefinitions;
		try {
			slf4jReporterDefinitions = GSON.fromJson(slf4jReporters, SLF4J_REPORTERS_TYPE);
		} catch (Exception e) {
			Slf4jReporterDefinition slf4jReporterDefinition = GSON.fromJson(slf4jReporters, SLF4J_REPORTER_TYPE);
			slf4jReporterDefinitions = new ArrayList<Slf4jReporterDefinition>();
			slf4jReporterDefinitions.add(slf4jReporterDefinition);
		}
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
	 * @param baseName
	 * @throws Exception
	 */
	private synchronized MetricGroup initializeMetricGroup(final String baseName) throws Exception {
		return initializeMetricGroup(this.name, null);
	}

	/**
	 * @param parameters
	 */
	private void warnIfTimingStopIsUsedWithOtherParameters(final Map<String, Object> parameters) {
		if (parameters.containsKey("timing")) {
			Object value = parameters.get("timing");
			if (value instanceof String) {
				String stringValue = (String) value;
				if (TimingAction.STOP.name().equalsIgnoreCase(stringValue) && parameters.size() > 1) {
					if (!parameters.containsKey("infix") || parameters.size() > 2) {
						LOGGER.warn(MARKER, "found timing={}, additional parameters may be ignored: {}", stringValue, parameters);
					}
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
	}

	@Override
	protected void doStop() throws Exception {
		LOGGER.debug(MARKER, "doStop()");
	}

	@Override
	protected void doSuspend() throws Exception {
		super.doSuspend();
		LOGGER.debug(MARKER, "doSuspend()");
	}

}
