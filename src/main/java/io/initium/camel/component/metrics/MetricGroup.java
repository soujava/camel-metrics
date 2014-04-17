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
import io.initium.camel.component.metrics.definition.metric.TimerDefinition;
import io.initium.camel.component.metrics.definition.reporter.ConsoleReporterDefinition;
import io.initium.camel.component.metrics.definition.reporter.CsvReporterDefinition;
import io.initium.camel.component.metrics.definition.reporter.GraphiteReporterDefinition;
import io.initium.camel.component.metrics.definition.reporter.JmxReporterDefinition;
import io.initium.camel.component.metrics.definition.reporter.ReporterDefinition;
import io.initium.camel.component.metrics.definition.reporter.Slf4jReporterDefinition;
import io.initium.common.util.MetricUtils;
import io.initium.common.util.StringUtils;

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
	private static final String						SELF							= Thread.currentThread().getStackTrace()[1].getClassName();
	private static final Logger						LOGGER							= LoggerFactory.getLogger(SELF);

	// defaults
	private static final List<TimeUnit>				SINCE_TIME_UNIT_VALUES			= Arrays.asList(TimeUnit.MILLISECONDS, TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS);
	private static final List<TimeUnit>				INTERVAL_TIME_UNIT_VALUES		= Arrays.asList(TimeUnit.MILLISECONDS, TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS);

	// fields
	private MetricsEndpoint							metricsEndpoint;
	private final MetricRegistry					metricRegistry;

	// default metrics
	private final Meter								exchangeRate;
	private long									lastExchangeTime				= System.nanoTime();
	private Exchange								lastExchange;
	private boolean									haveProcessedAtLeastOneExchange	= false;
	private final Map<TimeUnit, Histogram>			intervals;
	private Timer									timer;
	private TimerDefinition							timerDefinition;
	private final Set<Gauge>						sinceGauges						= new HashSet<Gauge>();

	// custom metrics
	private CounterDefinition						counterDefinition;
	private Counter									customCounter;
	private HistogramDefinition						histogramDefinition;
	private Histogram								customHistogram;
	private GaugeDefinition							gaugeDefinition;

	// reporters
	private final List<JmxReporter>					jmxReporters					= new ArrayList<JmxReporter>();
	private final List<ConsoleReporter>				consoleReporters				= new ArrayList<ConsoleReporter>();
	private final List<GraphiteReporter>			graphiteReporters				= new ArrayList<GraphiteReporter>();
	private final List<Slf4jReporter>				slf4jReporters					= new ArrayList<Slf4jReporter>();
	private final List<CsvReporter>					csvReporters					= new ArrayList<CsvReporter>();
	private final Map<String, ReporterDefinition>	componentReporterDefinitions;
	private List<ReporterDefinition>				reporterDefinitions;
	private String									baseName;
	private String									infixName;
	private String									fullName;
	private Exchange								creatingExchange;
	private CachedGauge								customGauge;

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
		String exchangeRateMetricName = MetricUtils.calculateFullMetricName(this.fullName, "rate");
		this.exchangeRate = this.metricRegistry.meter(exchangeRateMetricName);

		// since gauge
		for (final TimeUnit timeUnit : SINCE_TIME_UNIT_VALUES) {
			String sinceName = MetricUtils.calculateFullMetricName(this.fullName, "since" + getPrettyName(timeUnit));
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
		this.intervals = new HashMap<TimeUnit, Histogram>();
		for (final TimeUnit timeUnit : INTERVAL_TIME_UNIT_VALUES) {
			String lclName = MetricUtils.calculateFullMetricName(this.fullName, "interval" + getPrettyName(timeUnit));
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
	 * @param counterDefinition
	 */
	public void addCounterDefinition(final CounterDefinition counterDefinition) {
		this.counterDefinition = counterDefinition;
		String lclName = MetricUtils.calculateFullMetricName(this.fullName, this.counterDefinition.getName());
		LOGGER.debug(MARKER, "enabling custom counter metric: {}", lclName);
		this.customCounter = this.metricRegistry.counter(lclName);
	}

	/**
	 * @param gaugeDefinition
	 */
	public void addGaugeDefinition(final GaugeDefinition gaugeDefinition) {
		this.gaugeDefinition = gaugeDefinition;
		String lclName = MetricUtils.calculateFullMetricName(this.fullName, this.gaugeDefinition.getName());
		LOGGER.debug(MARKER, "enabling custom gauge metric: {}", lclName);
		if (this.gaugeDefinition instanceof CachedGaugeDefinition) {
			final CachedGaugeDefinition cachedGaugeDefinition = (CachedGaugeDefinition) this.gaugeDefinition;
			this.customGauge = new CachedGauge<Object>(cachedGaugeDefinition.getCacheDuration(), cachedGaugeDefinition.getCacheDurationUnit()) {
				@Override
				protected Object loadValue() {
					if (MetricGroup.this.lastExchange != null) {
						return cachedGaugeDefinition.getExpression().evaluate(MetricGroup.this.lastExchange, Object.class);
					} else {
						return null;
					}
				}
			};
			this.metricRegistry.register(lclName, this.customGauge);
		} else {
			LOGGER.warn(MARKER, "ignoring custom gauge, unsupported gauge definition: {}", this.gaugeDefinition);
		}
	}

	/**
	 * @param histogramDefinition
	 */
	public void addHistogramDefinition(final HistogramDefinition histogramDefinition) {
		this.histogramDefinition = histogramDefinition;
		String lclName = MetricUtils.calculateFullMetricName(this.fullName, this.histogramDefinition.getName());
		LOGGER.debug(MARKER, "enabling histogram counter metric: {}", lclName);
		this.customHistogram = this.metricRegistry.histogram(lclName);
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
		if (metric == this.exchangeRate) {
			return true;
		} else if (metric == this.timer) {
			return true;
		} else if (metric == this.customCounter) {
			return true;
		} else if (metric == this.customHistogram) {
			return true;
		} else if (metric == this.customGauge) {
			return true;
		}
		for (Histogram histogram : this.intervals.values()) {
			if (metric == histogram) {
				return true;
			}
		}
		return this.sinceGauges.contains(metric);
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
		this.exchangeRate.mark();
		if (this.haveProcessedAtLeastOneExchange) {
			LOGGER.trace("deltaInNanos: {}", deltaInNanos);
			updateAllIntervals(deltaInNanos);
		}
		this.haveProcessedAtLeastOneExchange = true;
		markCustomCounters(exchange);
		markCustomHistograms(exchange);
	}

	/**
	 * @param timeUnit
	 * @return
	 */
	private String getPrettyName(final TimeUnit timeUnit) {
		return StringUtils.capitalize(timeUnit.toString());
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
	private void markCustomCounters(final Exchange exchange) {
		if (this.counterDefinition != null) {
			if (this.customCounter != null) {
				Long value = this.counterDefinition.getExpression().evaluate(exchange, Long.class);
				if (value != null) {
					this.customCounter.inc(value);
				} else {
					LOGGER.warn(MARKER, "ignoring attempt to increment custom counter by non-Long");
				}
			} else {
				LOGGER.warn(MARKER, "ignoring attempt to increment a null custom counter");
			}
		}
	}

	/**
	 * @param exchange
	 */
	private void markCustomHistograms(final Exchange exchange) {
		if (this.histogramDefinition != null) {
			if (this.customHistogram != null) {
				Long valueLong = this.histogramDefinition.getExpression().evaluate(exchange, Long.class);
				if (valueLong != null) {
					this.customHistogram.update(valueLong);
				} else {
					LOGGER.warn(MARKER, "ignoring attempt to update custom histogram by non-Long");
				}
			} else {
				LOGGER.warn(MARKER, "ignoring attempt to update a null custom histogram");
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
