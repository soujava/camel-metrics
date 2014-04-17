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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.Language;
import org.apache.camel.spi.UriParam;
import org.apache.camel.util.EndpointHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import io.initium.camel.component.metrics.definition.metric.CachedGaugeDefinition;
import io.initium.camel.component.metrics.definition.metric.CounterDefinition;
import io.initium.camel.component.metrics.definition.metric.HistogramDefinition;
import io.initium.camel.component.metrics.definition.metric.TimerDefinition;
import io.initium.camel.component.metrics.definition.reporter.ConsoleReporterDefinition;
import io.initium.camel.component.metrics.definition.reporter.CsvReporterDefinition;
import io.initium.camel.component.metrics.definition.reporter.GraphiteReporterDefinition;
import io.initium.camel.component.metrics.definition.reporter.JmxReporterDefinition;
import io.initium.camel.component.metrics.definition.reporter.ReporterDefinition;
import io.initium.camel.component.metrics.definition.reporter.Slf4jReporterDefinition;
import io.initium.common.util.MetricUtils;
import io.initium.common.util.OptionHelper;

import static io.initium.camel.component.metrics.MetricsComponent.MARKER;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.0
 * @since 2014-02-19
 */
@SuppressWarnings({"rawtypes"})
@ManagedResource(description = "Managed MetricsEndpoint")
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

	private Expression						infixExpression;
	private boolean							isInternalTimerEnabled	= false;
	private final Timer						internalTimerStart		= null;
	private final Timer						internalTimerNoop		= null;

	private final Timer						internalTimerStop		= null;
	// for timer metric
	private final Timer						timer					= null;
	@UriParam
	private String							timingName				= "timing";
	private String							timingActionName		= null;
	private TimingAction					timingAction			= TimingAction.NOOP;
	// for expression based counter metric
	private String							counterName				= "count";
	private Expression						counterExpression		= null;
	// for expression based histogram metric
	private String							histogramName			= "histogram";
	private Expression						histogramExpression		= null;
	// for expression based gauge metric
	private String							gaugeName				= "gauge";
	private Expression						gaugeExpression			= null;
	private long							gaugeCacheDuration		= 10;
	private TimeUnit						gaugeCacheDurationUnit	= TimeUnit.SECONDS;
	// for reporters
	private static final Gson				gson;
	private static final Type				JMX_REPORTERS_TYPE		= new TypeToken<Collection<JmxReporterDefinition>>() {}.getType();
	private static final Type				CONSOLE_REPORTERS_TYPE	= new TypeToken<Collection<ConsoleReporterDefinition>>() {}.getType();
	private static final Type				GRAPHITE_REPORTERS_TYPE	= new TypeToken<Collection<GraphiteReporterDefinition>>() {}.getType();
	private static final Type				SLF4J_REPORTERS_TYPE	= new TypeToken<Collection<Slf4jReporterDefinition>>() {}.getType();
	private static final Type				CSV_REPORTERS_TYPE		= new TypeToken<Collection<CsvReporterDefinition>>() {}.getType();
	private final List<ReporterDefinition>	reporterDefinitions		= new ArrayList<ReporterDefinition>();

	static {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(TimeUnit.class, new TimeUnitConverter());
		gson = gsonBuilder.create();
	}

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
		warnIfTimingStopIsUsedWithOtherParameters(parameters);
		EndpointHelper.setProperties(getCamelContext(), this, parameters);
		switch (this.timingAction) {
			case STOP:
				LOGGER.debug(MARKER, "skipping initialization, timingAction={}", this.timingAction);
				break;
			default:
				this.metricsComponent.registerName(this.name);
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
	 * @return the infixExpression
	 */
	public Expression getInfixExpression() {
		return this.infixExpression;
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
	 * @return the reporterDefinitions
	 */
	public List<ReporterDefinition> getReporterDefinitions() {
		return this.reporterDefinitions;
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
		metricGroup = new MetricGroup(this, baseName, infixName);
		// timer
		if (this.timingAction == TimingAction.START) {
			TimerDefinition timerDefinition = new TimerDefinition(this.timingName);
			metricGroup.addTimerDefinition(timerDefinition);
		}
		// expression based counter
		if (this.counterExpression != null) {
			CounterDefinition counterDefinition = new CounterDefinition(this.counterName, this.counterExpression);
			metricGroup.addCounterDefinition(counterDefinition);
		}
		// expression based histogram
		if (this.histogramExpression != null) {
			HistogramDefinition histogramDefinition = new HistogramDefinition(this.histogramName, this.histogramExpression);
			metricGroup.addHistogramDefinition(histogramDefinition);
		}
		// expression based gauge
		if (this.gaugeExpression != null) {
			CachedGaugeDefinition cachedGaugeDefinition = new CachedGaugeDefinition(this.gaugeName, this.gaugeExpression, this.gaugeCacheDuration, this.gaugeCacheDurationUnit);
			metricGroup.addGaugeDefinition(cachedGaugeDefinition);
		}
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
			TimerDefinition timerDefinition = new TimerDefinition(this.timingName);
			metricGroup.addTimerDefinition(timerDefinition);
		}
		// expression based counter
		if (this.counterExpression != null) {
			CounterDefinition counterDefinition = new CounterDefinition(this.counterName, this.counterExpression);
			metricGroup.addCounterDefinition(counterDefinition);
		}
		// expression based histogram
		if (this.histogramExpression != null) {
			HistogramDefinition histogramDefinition = new HistogramDefinition(this.histogramName, this.histogramExpression);
			metricGroup.addHistogramDefinition(histogramDefinition);
		}
		// expression based gauge
		if (this.gaugeExpression != null) {
			CachedGaugeDefinition cachedGaugeDefinition = new CachedGaugeDefinition(this.gaugeName, this.gaugeExpression, this.gaugeCacheDuration, this.gaugeCacheDurationUnit);
			metricGroup.addGaugeDefinition(cachedGaugeDefinition);
		}
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
	 * @param consoleReporters
	 *            the consoleReporters to set
	 */
	public void setConsoleReporters(final String consoleReporters) {
		List<ConsoleReporterDefinition> consoleReporterDefinitions = gson.fromJson(consoleReporters, CONSOLE_REPORTERS_TYPE);
		for (ConsoleReporterDefinition consoleReporterDefinition : consoleReporterDefinitions) {
			this.reporterDefinitions.add(consoleReporterDefinition);
		}
	}

	/**
	 * @param counterExpression
	 *            the counterExpression to set
	 */
	public void setCounterDelta(final String counterExpression) {
		this.counterExpression = createFileLanguageExpression(counterExpression);
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
		List<CsvReporterDefinition> csvReporterDefinitions = gson.fromJson(csvReporters, CSV_REPORTERS_TYPE);
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
		this.gaugeExpression = createFileLanguageExpression(gaugeValue);
	}

	/**
	 * @param graphiteReporters
	 *            the graphiteReporters to set
	 */
	public void setGraphiteReporters(final String graphiteReporters) {
		List<GraphiteReporterDefinition> graphiteReporterDefinitions = gson.fromJson(graphiteReporters, GRAPHITE_REPORTERS_TYPE);
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
	 * @param histogramExpression
	 *            the histogramExpression to set
	 */
	public void setHistogramValue(final String histogramExpression) {
		this.histogramExpression = createFileLanguageExpression(histogramExpression);
	}

	/**
	 * @param infix
	 *            the infix to set
	 */
	public void setInfix(final String infix) {
		this.infixExpression = createFileLanguageExpression(infix);
	}

	/**
	 * @param jmxReporters
	 *            the jmxReporters to set
	 */
	public void setJmxReporters(final String jmxReporters) {
		List<JmxReporterDefinition> jmxReporterDefinitions = gson.fromJson(jmxReporters, JMX_REPORTERS_TYPE);
		for (JmxReporterDefinition jmxReporterDefinition : jmxReporterDefinitions) {
			this.reporterDefinitions.add(jmxReporterDefinition);
		}
	}

	/**
	 * @param slf4jReporters
	 *            the slf4jReporters to set
	 */
	public void setSlf4jReporters(final String slf4jReporters) {
		List<Slf4jReporterDefinition> slf4jReporterDefinitions = gson.fromJson(slf4jReporters, SLF4J_REPORTERS_TYPE);
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
