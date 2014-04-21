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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.support.ServiceSupport;
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
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.graphite.GraphiteReporter;

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
import io.initium.common.util.MetricUtils;

import static io.initium.camel.component.metrics.MetricsComponent.MARKER;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.2
 * @since 2014-02-19
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class MetricGroup extends ServiceSupport {

	// logging
	private static final String								SELF								= Thread.currentThread().getStackTrace()[1].getClassName();
	private static final Logger								LOGGER								= LoggerFactory.getLogger(SELF);

	// defaults
	private static final List<TimeUnit>						DEFAULT_SINCE_TIME_UNIT_VALUES		= Arrays.asList(TimeUnit.MILLISECONDS, TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS);
	private static final List<TimeUnit>						DEFAULT_INTERVAL_TIME_UNIT_VALUES	= Arrays.asList(TimeUnit.MILLISECONDS, TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS);

	// fields
	private MetricsEndpoint									metricsEndpoint;
	private final MetricRegistry							metricRegistry;
	private String											baseName;
	private String											infixName;
	private String											fullName;
	private Exchange										creatingExchange;

	// default metrics
	private final Meter										rate;
	private long											lastExchangeTime					= System.nanoTime();
	private Exchange										lastExchange;
	private boolean											haveProcessedAtLeastOneExchange		= false;
	private final Map<TimeUnit, Histogram>					intervals							= new HashMap<TimeUnit, Histogram>();
	private Timer											timer;
	private TimerDefinition									timerDefinition;
	private final Set<Gauge>								sinceGauges							= new HashSet<Gauge>();

	// for expression based metrics
	private final Map<HistogramDefinition, Histogram>		histograms							= new HashMap<HistogramDefinition, Histogram>();
	private final Map<CounterDefinition, Counter>			counters							= new HashMap<CounterDefinition, Counter>();
	private final Map<MeterDefinition, Meter>				meters								= new HashMap<MeterDefinition, Meter>();
	private final Map<GaugeDefinition, Gauge>				gauges								= new HashMap<GaugeDefinition, Gauge>();
	private final Map<CachedGaugeDefinition, CachedGauge>	cachedGauges						= new HashMap<CachedGaugeDefinition, CachedGauge>();

	// reporter definitions
	private final Map<String, ReporterDefinition>			componentReporterDefinitions;
	private List<ReporterDefinition>						reporterDefinitions;

	// active reporters
	private final List<JmxReporter>							jmxReporters						= new ArrayList<JmxReporter>();
	private final List<ConsoleReporter>						consoleReporters					= new ArrayList<ConsoleReporter>();
	private final List<GraphiteReporter>					graphiteReporters					= new ArrayList<GraphiteReporter>();
	private final List<Slf4jReporter>						slf4jReporters						= new ArrayList<Slf4jReporter>();
	private final List<CsvReporter>							csvReporters						= new ArrayList<CsvReporter>();

	/**
	 * @param metricsEndpoint
	 * @param baseName
	 * @param infixName
	 */
	public MetricGroup(final MetricsEndpoint metricsEndpoint, final String baseName, final String infixName) {
		this.metricsEndpoint = metricsEndpoint;
		this.baseName = baseName;
		this.infixName = infixName;
		this.fullName = MetricUtils.calculateFullMetricName(baseName, infixName);

		// derived
		this.metricRegistry = this.metricsEndpoint.getMetricsRegistry();
		this.componentReporterDefinitions = this.metricsEndpoint.getMetricComponent().getReporterDefinitions();
		this.reporterDefinitions = this.metricsEndpoint.getReporterDefinitions();

		// rate meter
		String rateMetricName = MetricUtils.calculateFullMetricName(this.fullName, this.metricsEndpoint.getRateName());
		this.rate = this.metricRegistry.meter(rateMetricName);

		// since gauge
		List<TimeUnit> sinceTimeUnitValues;
		List<TimeUnit> sinceTimeUnitValuesEndpoint = this.metricsEndpoint.getSinceTimeUnits();
		if (sinceTimeUnitValuesEndpoint != null) {
			sinceTimeUnitValues = sinceTimeUnitValuesEndpoint;
		} else {
			sinceTimeUnitValues = DEFAULT_SINCE_TIME_UNIT_VALUES;
		}
		for (final TimeUnit timeUnit : sinceTimeUnitValues) {
			// TOOD change this?
			String sinceName = MetricUtils.calculateFullMetricName(this.fullName, this.metricsEndpoint.getSinceName() + '.' + getPrettyName(timeUnit));
			Gauge sinceGauge = new Gauge<Double>() {
				@Override
				public Double getValue() {
					return lastExchangeDelta(timeUnit);
				}
			};
			this.sinceGauges.add(sinceGauge);
			this.metricRegistry.register(sinceName, sinceGauge);
		}

		// interval histogram
		List<TimeUnit> intervalTimeUnitValues;
		List<TimeUnit> intervalTimeUnitValuesEndpoint = this.metricsEndpoint.getIntervalTimeUnits();
		if (intervalTimeUnitValuesEndpoint != null) {
			intervalTimeUnitValues = intervalTimeUnitValuesEndpoint;
		} else {
			intervalTimeUnitValues = DEFAULT_INTERVAL_TIME_UNIT_VALUES;
		}
		for (final TimeUnit timeUnit : intervalTimeUnitValues) {
			String lclName = MetricUtils.calculateFullMetricName(this.fullName, this.metricsEndpoint.getIntervalName() + '.' + getPrettyName(timeUnit));
			Histogram intervalHistogram = new Histogram(new ExponentiallyDecayingReservoir());
			this.metricRegistry.register(lclName, intervalHistogram);
			this.intervals.put(timeUnit, intervalHistogram);
		}

	}

	/**
	 * @param metricsEndpoint
	 * @param baseName
	 * @param infixName
	 * @param creatingExchange
	 */
	public MetricGroup(final MetricsEndpoint metricsEndpoint, final String baseName, final String infixName, final Exchange creatingExchange) {
		this(metricsEndpoint, baseName, infixName);
		this.creatingExchange = creatingExchange;
	}

	/**
	 * @param cachedGaugeDefinition
	 */
	public void addCachedGaugeDefinition(final CachedGaugeDefinition cachedGaugeDefinition) {
		if (cachedGaugeDefinition != null) {
			String subName = cachedGaugeDefinition.getName();
			if (subName == null) {
				subName = CachedGaugeDefinition.getNextDefaultName();
			}
			String lclName = MetricUtils.calculateFullMetricName(this.fullName, subName);
			LOGGER.debug(MARKER, "enabling cached gauge metric: {} based on definition: {}", lclName, cachedGaugeDefinition);
			CachedGauge<String> cachedGauge = new CachedGauge<String>(cachedGaugeDefinition.getDuration(), cachedGaugeDefinition.getDurationUnit()) {
				@Override
				protected String loadValue() {
					if (MetricGroup.this.lastExchange != null) {
						Object result = cachedGaugeDefinition.getExpression().evaluate(MetricGroup.this.lastExchange, Object.class);
						try {
							return (String) result;
						} catch (Exception e) {
							LOGGER.warn("result did not evalaute to a String, result={}", result);
							return null;
						}
					} else {
						LOGGER.info("no exchange available for gauge calculation");
						return null;
					}
				}
			};
			this.metricRegistry.register(lclName, cachedGauge);
			this.cachedGauges.put(cachedGaugeDefinition, cachedGauge);
		}
	}

	/**
	 * @param cachedGaugeDefinitions
	 */
	public void addCachedGaugeDefinitions(final List<CachedGaugeDefinition> cachedGaugeDefinitions) {
		if (cachedGaugeDefinitions != null) {
			for (CachedGaugeDefinition cachedGaugeDefinition : cachedGaugeDefinitions) {
				addCachedGaugeDefinition(cachedGaugeDefinition);
			}
		}
	}

	/**
	 * @param counterDefinition
	 */
	public void addCounterDefinition(final CounterDefinition counterDefinition) {
		if (counterDefinition != null) {
			String subName = counterDefinition.getName();
			if (subName == null) {
				subName = CounterDefinition.getNextDefaultName();
			}
			String lclName = MetricUtils.calculateFullMetricName(this.fullName, subName);
			LOGGER.debug(MARKER, "enabling counter metric: {} based on definition: {}", lclName, counterDefinition);
			Counter counter = this.metricRegistry.counter(lclName);
			this.counters.put(counterDefinition, counter);
		}
	}

	/**
	 * @param counterDefinitions
	 */
	public void addCounterDefinitions(final List<CounterDefinition> counterDefinitions) {
		if (counterDefinitions != null) {
			for (CounterDefinition counterDefinition : counterDefinitions) {
				addCounterDefinition(counterDefinition);
			}
		}
	}

	/**
	 * @param gaugeDefinition
	 */
	public void addGaugeDefinition(final GaugeDefinition gaugeDefinition) {
		if (gaugeDefinition != null) {
			String subName = gaugeDefinition.getName();
			if (subName == null) {
				subName = GaugeDefinition.getNextDefaultName();
			}
			String lclName = MetricUtils.calculateFullMetricName(this.fullName, subName);
			LOGGER.debug(MARKER, "enabling gauge metric: {} based on definition: {}", lclName, gaugeDefinition);
			Gauge<String> gauge = new Gauge<String>() {
				@Override
				public String getValue() {
					if (MetricGroup.this.lastExchange != null) {
						Object result = gaugeDefinition.getExpression().evaluate(MetricGroup.this.lastExchange, Object.class);
						try {
							return (String) result;
						} catch (Exception e) {
							LOGGER.warn("result did not evalaute to a String, result={}", result);
							return null;
						}
					} else {
						LOGGER.info("no exchange available for gauge calculation");
						return null;
					}
				}
			};
			this.metricRegistry.register(lclName, gauge);
			this.gauges.put(gaugeDefinition, gauge);
		}
	}

	/**
	 * @param gaugeDefinitions
	 */
	public void addGaugeDefinitions(final List<GaugeDefinition> gaugeDefinitions) {
		if (gaugeDefinitions != null) {
			for (GaugeDefinition gaugeDefinition : gaugeDefinitions) {
				addGaugeDefinition(gaugeDefinition);
			}
		}
	}

	/**
	 * @param histogramDefinition
	 */
	public void addHistogramDefinition(final HistogramDefinition histogramDefinition) {
		if (histogramDefinition != null) {
			String subName = histogramDefinition.getName();
			if (subName == null) {
				subName = HistogramDefinition.getNextDefaultName();
			}
			String lclName = MetricUtils.calculateFullMetricName(this.fullName, subName);
			LOGGER.debug(MARKER, "enabling histogram metric: {} based on definition: {}", lclName, histogramDefinition);
			Histogram histogram = this.metricRegistry.histogram(lclName);
			this.histograms.put(histogramDefinition, histogram);
		}
	}

	/**
	 * @param histogramDefinitions
	 */
	public void addHistogramDefinitions(final Collection<HistogramDefinition> histogramDefinitions) {
		if (histogramDefinitions != null) {
			for (HistogramDefinition histogramDefinition : histogramDefinitions) {
				addHistogramDefinition(histogramDefinition);
			}
		}
	}

	/**
	 * @param meterDefinition
	 */
	public void addMeterDefinition(final MeterDefinition meterDefinition) {
		if (meterDefinition != null) {
			String subName = meterDefinition.getName();
			if (subName == null) {
				subName = MeterDefinition.getNextDefaultName();
			}
			String lclName = MetricUtils.calculateFullMetricName(this.fullName, subName);
			LOGGER.debug(MARKER, "enabling meter metric: {} based on definition: {}", lclName, meterDefinition);
			Meter meter = this.metricRegistry.meter(lclName);
			this.meters.put(meterDefinition, meter);
		}
	}

	/**
	 * @param meterDefinitions
	 */
	public void addMeterDefinitions(final List<MeterDefinition> meterDefinitions) {
		if (meterDefinitions != null) {
			for (MeterDefinition meterDefinition : meterDefinitions) {
				addMeterDefinition(meterDefinition);
			}
		}
	}

	/**
	 * @param timerDefinition
	 */
	public void addTimerDefinition(final TimerDefinition timerDefinition) {
		this.timerDefinition = timerDefinition;
		String lclName = MetricUtils.calculateFullMetricName(this.fullName, this.timerDefinition.getName());
		LOGGER.debug(MARKER, "enabling timer metric: {}", lclName);
		this.timer = this.metricRegistry.timer(lclName);
	}

	/**
	 * @param metric
	 * @return
	 */
	public boolean contains(final Metric metric) {
		if (metric == this.rate) {
			return true;
		} else if (this.intervals.values().contains(metric)) {
			return true;
		} else if (this.sinceGauges.contains(metric)) {
			return true;
		} else if (metric == this.timer) {
			return true;
		} else if (this.counters.values().contains(metric)) {
			return true;
		} else if (this.histograms.values().contains(metric)) {
			return true;
		} else if (this.meters.values().contains(metric)) {
			return true;
		} else if (this.gauges.values().contains(metric)) {
			return true;
		} else if (this.cachedGauges.values().contains(metric)) {
			return true;
		}
		return false;
	}

	/**
	 * @return the baseName
	 */
	public String getBaseName() {
		return this.baseName;
	}

	/**
	 * @return the fullName
	 */
	public String getFullName() {
		return this.fullName;
	}

	/**
	 * @return the infixName
	 */
	public String getInfixName() {
		return this.infixName;
	}

	/**
	 * @return
	 */
	public Timer getTimer() {
		return this.timer;
	}

	/**
	 * @return
	 */
	public String getTimingName() {
		if (this.timerDefinition != null) {
			return this.timerDefinition.getName();
		}
		return null;
	}

	/**
	 * 
	 */
	public void mark(final Exchange exchange) {
		long deltaInNanos = lastExchangeDelta();
		this.lastExchange = exchange;
		this.lastExchangeTime = System.nanoTime();
		this.rate.mark();
		if (this.haveProcessedAtLeastOneExchange) {
			LOGGER.trace("deltaInNanos: {}", deltaInNanos);
			updateAllIntervals(deltaInNanos);
		}
		this.haveProcessedAtLeastOneExchange = true;
		markCounters(exchange);
		markMeters(exchange);
		markHistograms(exchange);
	}

	/**
	 * @param timeUnit
	 * @return
	 */
	private String getPrettyName(final TimeUnit timeUnit) {
		// return StringUtils.capitalize(timeUnit.toString());
		return timeUnit.toString().toLowerCase();
	}

	/**
	 * @return
	 */
	private long lastExchangeDelta() {
		return System.nanoTime() - this.lastExchangeTime;
	}

	/**
	 * @return
	 */
	private double lastExchangeDelta(final TimeUnit timeUnit) {
		return (double) lastExchangeDelta() / timeUnit.toNanos(1);
	}

	/**
	 * @param exchange
	 */
	private void markCounters(final Exchange exchange) {
		for (Entry<CounterDefinition, Counter> entry : this.counters.entrySet()) {
			CounterDefinition counterDefinition = entry.getKey();
			Counter counter = entry.getValue();
			if (counterDefinition != null) {
				if (counter != null) {
					Long valueLong = counterDefinition.getExpression().evaluate(exchange, Long.class);
					if (valueLong != null) {
						counter.inc(valueLong);
					} else {
						LOGGER.warn(MARKER, "ignoring attempt to increment custom counter by non-Long");
					}
				} else {
					LOGGER.warn(MARKER, "ignoring attempt to increment a null custom counter");
				}
			}
		}
	}

	/**
	 * @param exchange
	 */
	private void markHistograms(final Exchange exchange) {
		for (Entry<HistogramDefinition, Histogram> entry : this.histograms.entrySet()) {
			HistogramDefinition histogramDefinition = entry.getKey();
			Histogram histogram = entry.getValue();
			if (histogramDefinition != null) {
				if (histogram != null) {
					Long valueLong = histogramDefinition.getExpression().evaluate(exchange, Long.class);
					if (valueLong != null) {
						histogram.update(valueLong);
					} else {
						LOGGER.warn(MARKER, "ignoring attempt to update histogram by non-Long");
					}
				} else {
					LOGGER.warn(MARKER, "ignoring attempt to update a null histogram");
				}
			}
		}
	}

	/**
	 * @param exchange
	 */
	private void markMeters(final Exchange exchange) {
		for (Entry<MeterDefinition, Meter> entry : this.meters.entrySet()) {
			MeterDefinition meterDefinition = entry.getKey();
			Meter meter = entry.getValue();
			if (meterDefinition != null) {
				if (meter != null) {
					Long valueLong = meterDefinition.getExpression().evaluate(exchange, Long.class);
					if (valueLong != null) {
						meter.mark(valueLong);
					} else {
						LOGGER.warn(MARKER, "ignoring attempt to mark custom by non-Long");
					}
				} else {
					LOGGER.warn(MARKER, "ignoring attempt to mark a null custom meter");
				}
			}
		}
	}

	/**
	 * @param reporterDefinition
	 */
	private void registerAndStart(final ReporterDefinition reporterDefinition) {
		if (reporterDefinition instanceof JmxReporterDefinition) {
			JmxReporterDefinition jmxReporterDefinition = ((JmxReporterDefinition) reporterDefinition).getReporterDefinitionWithDefaults();
			LOGGER.info(MARKER, "adding JmxReporterDefinition: {}", jmxReporterDefinition);
			JmxReporter jmxReporter = jmxReporterDefinition.buildReporter(this.metricRegistry, this.creatingExchange, this);
			this.jmxReporters.add(jmxReporter);
			LOGGER.info(MARKER, "starting reporter: {}", jmxReporter);
			jmxReporter.start();
		} else if (reporterDefinition instanceof ConsoleReporterDefinition) {
			ConsoleReporterDefinition consoleReporterDefinition = ((ConsoleReporterDefinition) reporterDefinition).getReporterDefinitionWithDefaults();
			LOGGER.info(MARKER, "adding ConsoleReporterDefinition: {}", consoleReporterDefinition);
			ConsoleReporter consoleReporter = consoleReporterDefinition.buildReporter(this.metricRegistry, this.creatingExchange, this);
			this.consoleReporters.add(consoleReporter);
			LOGGER.info(MARKER, "starting reporter: {}", consoleReporter);
			consoleReporter.start(consoleReporterDefinition.getPeriodDuration(), consoleReporterDefinition.getPeriodDurationUnit());
		} else if (reporterDefinition instanceof GraphiteReporterDefinition) {
			GraphiteReporterDefinition graphiteReporterDefinition = ((GraphiteReporterDefinition) reporterDefinition).getReporterDefinitionWithDefaults();
			LOGGER.info(MARKER, "adding GraphiteReporterDefinition: {}", graphiteReporterDefinition);
			GraphiteReporter graphiteReporter = graphiteReporterDefinition.buildReporter(this.metricRegistry, this.creatingExchange, this);
			this.graphiteReporters.add(graphiteReporter);
			LOGGER.info(MARKER, "starting reporter: {}", graphiteReporter);
			graphiteReporter.start(graphiteReporterDefinition.getPeriodDuration(), graphiteReporterDefinition.getPeriodDurationUnit());
		} else if (reporterDefinition instanceof Slf4jReporterDefinition) {
			Slf4jReporterDefinition slf4jReporterDefinition = ((Slf4jReporterDefinition) reporterDefinition).getReporterDefinitionWithDefaults();
			LOGGER.info(MARKER, "adding Slf4jReporterDefinition: {}", slf4jReporterDefinition);
			Slf4jReporter slf4jReporter = slf4jReporterDefinition.buildReporter(this.metricRegistry, this.creatingExchange, this);
			this.slf4jReporters.add(slf4jReporter);
			LOGGER.info(MARKER, "starting reporter: {}", slf4jReporter);
			slf4jReporter.start(slf4jReporterDefinition.getPeriodDuration(), slf4jReporterDefinition.getPeriodDurationUnit());
		} else if (reporterDefinition instanceof CsvReporterDefinition) {
			CsvReporterDefinition csvReporterDefinition = ((CsvReporterDefinition) reporterDefinition).getReporterDefinitionWithDefaults();
			LOGGER.info(MARKER, "adding CsvjReporterDefinition: {}", csvReporterDefinition);
			CsvReporter csvReporter = csvReporterDefinition.buildReporter(this.metricRegistry, this.creatingExchange, this);
			this.csvReporters.add(csvReporter);
			LOGGER.info(MARKER, "starting reporter: {}", csvReporter);
			csvReporter.start(csvReporterDefinition.getPeriodDuration(), csvReporterDefinition.getPeriodDurationUnit());
		} else {
			LOGGER.warn(MARKER, "unsupported ReporterDefinition: {}: {}", reporterDefinition.getClass(), reporterDefinition);
		}
	}

	/**
	 *
	 */
	private void startReporters() {
		boolean oneReporterHasBeenStarted = false;
		Map<String, ReporterDefinition> leftoverReporterDefinitions = new HashMap<String, ReporterDefinition>();
		leftoverReporterDefinitions.putAll(this.componentReporterDefinitions);

		// check component definitions for defaults
		for (ReporterDefinition reporterDefinition : this.reporterDefinitions) {
			String reporterDefinitionName = reporterDefinition.getName();
			ReporterDefinition componentReporterDefinition = leftoverReporterDefinitions.get(reporterDefinitionName);
			if (componentReporterDefinition != null) {
				ReporterDefinition combinedReporterDefinition = componentReporterDefinition;
				combinedReporterDefinition = componentReporterDefinition.applyAsOverride(reporterDefinition);
				registerAndStart(combinedReporterDefinition);
				oneReporterHasBeenStarted = true;
				leftoverReporterDefinitions.remove(reporterDefinitionName);
			} else {
				registerAndStart(reporterDefinition);
				oneReporterHasBeenStarted = true;
			}
		}
		// start the remaining definitions
		for (Entry<String, ReporterDefinition> leftoverReporterDefinitionEntry : leftoverReporterDefinitions.entrySet()) {
			ReporterDefinition reporterDefinition = leftoverReporterDefinitionEntry.getValue();
			registerAndStart(reporterDefinition);
			oneReporterHasBeenStarted = true;
		}
		if (!oneReporterHasBeenStarted) {
			registerAndStart(Slf4jReporterDefinition.getDefaultReporter());
		}
	}

	/**
	 *
	 */
	private void stopReporters() {
		for (JmxReporter jmxReporter : this.jmxReporters) {
			jmxReporter.stop();
		}
		this.jmxReporters.clear();
		for (ConsoleReporter consoleReporter : this.consoleReporters) {
			consoleReporter.stop();
		}
		this.consoleReporters.clear();
		for (GraphiteReporter graphiteReporter : this.graphiteReporters) {
			graphiteReporter.stop();
		}
		this.graphiteReporters.clear();
		for (Slf4jReporter slf4jReporter : this.slf4jReporters) {
			slf4jReporter.stop();
		}
		this.slf4jReporters.clear();
		for (CsvReporter csvReporter : this.csvReporters) {
			csvReporter.stop();
		}
		this.csvReporters.clear();
	}

	/**
	 * @param deltaInNanos
	 */
	private void updateAllIntervals(final long deltaInNanos) {
		for (Entry<TimeUnit, Histogram> entry : this.intervals.entrySet()) {
			Histogram interval = entry.getValue();
			long delta = entry.getKey().convert(deltaInNanos, TimeUnit.NANOSECONDS);
			interval.update(delta);
		}
	}

	@Override
	protected void doStart() throws Exception {
		LOGGER.debug(MARKER, "doStart()");
		// stop and clear reporters (if any)
		stopReporters();
		// create and start new reporters
		startReporters();
	}

	@Override
	protected void doStop() throws Exception {
		// stop and clear reporters
		stopReporters();
	}
}
