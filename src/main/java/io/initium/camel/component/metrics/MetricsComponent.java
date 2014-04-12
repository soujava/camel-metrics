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
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.camel.Endpoint;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.UriEndpointComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.GraphiteReporter;

import io.initium.camel.component.metrics.LoggingMetricRegistryListener.Level;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.0
 * @since 2014-02-19
 */
public class MetricsComponent extends UriEndpointComponent {

	// logging
	private static final String									SELF							= Thread.currentThread().getStackTrace()[1].getClassName();
	private static final Logger									LOGGER							= LoggerFactory.getLogger(SELF);

	// constants
	public static final Marker									MARKER							= MarkerFactory.getMarker("METRICS");
	public static final String									DEFAULT_JMX_DOMAIN				= "metrics";
	public static final String									TIMING_MAP_NAME					= DEFAULT_JMX_DOMAIN + ".TimingMap";

	// fields
	private final MetricRegistry								metricRegistry;

	// reporters
	private final Map<String, ReporterDefinition>				componentReporterDefinitions	= new HashMap<String, ReporterDefinition>();
	private final Map<String, Map<String, ReporterDefinition>>	metricReporterDefinitions		= new HashMap<String, Map<String, ReporterDefinition>>();
	private final Set<JmxReporter>								jmxReporters					= new HashSet<JmxReporter>();
	private final Set<ConsoleReporter>							consoleReporters				= new HashSet<ConsoleReporter>();
	private final Set<GraphiteReporter>							graphiteReporters				= new HashSet<GraphiteReporter>();

	// TODO stop them all

	// TODO on action = stop, warn if other parameters are non-null
	// TODO verify we have no reporters and stop them (or whatever...)

	/**
	 * 
	 */
	public MetricsComponent(final ReporterDefinition... newReporterDefinitions) {
		super(MetricsEndpoint.class);
		LOGGER.info(MARKER, "MetricsComponent({})", (Object[]) newReporterDefinitions);
		this.metricRegistry = new MetricRegistry();
		this.metricRegistry.addListener(new LoggingMetricRegistryListener(LOGGER, MARKER, Level.INFO));
		for (ReporterDefinition reporterDefinition : newReporterDefinitions) {
			String reporterDefinitionName = reporterDefinition.getName();
			if (this.componentReporterDefinitions.containsKey(reporterDefinitionName)) {
				throw new RuntimeCamelException("duplicate ReporterDefinition encountered: " + reporterDefinitionName);
			}
			this.componentReporterDefinitions.put(reporterDefinitionName, reporterDefinition);
		}
	}

	/**
	 * @return
	 */
	public MetricRegistry getMetricRegistry() {
		return this.metricRegistry;
	}

	/**
	 * @param name
	 */
	public synchronized void registerName(final String name, final Map<String, ReporterDefinition> reporterDefinitions) {
		if (this.metricReporterDefinitions.containsKey(name)) {
			throw new RuntimeCamelException("duplicate metric name found: " + name);
		}
		this.metricReporterDefinitions.put(name, reporterDefinitions);
	}

	/**
	 * @param reporterDefinition
	 */
	private void registerAndStart(final ReporterDefinition reporterDefinition) {
		if (reporterDefinition instanceof JmxReporterDefinition) {
			JmxReporterDefinition jmxReporterDefinition = ((JmxReporterDefinition) reporterDefinition).getReporterDefinitionWithDefaults();
			LOGGER.info("adding JmxReporterDefinition: {}", jmxReporterDefinition);
			JmxReporter jmxReporter = jmxReporterDefinition.buildReporter(this.metricRegistry);
			this.jmxReporters.add(jmxReporter);
			jmxReporter.start();
		} else if (reporterDefinition instanceof ConsoleReporterDefinition) {
			ConsoleReporterDefinition consoleReporterDefinition = ((ConsoleReporterDefinition) reporterDefinition).getReporterDefinitionWithDefaults();
			LOGGER.info("adding ConsoleReporterDefinition: {}", consoleReporterDefinition);
			ConsoleReporter consoleReporter = consoleReporterDefinition.buildReporter(this.metricRegistry);
			this.consoleReporters.add(consoleReporter);
			consoleReporter.start(consoleReporterDefinition.getPeriodDuration(), consoleReporterDefinition.getPeriodDurationUnit());
		} else if (reporterDefinition instanceof GraphiteReporterDefinition) {
			GraphiteReporterDefinition graphiteReporterDefinition = ((GraphiteReporterDefinition) reporterDefinition).getReporterDefinitionWithDefaults();
			LOGGER.info("adding GraphiteReporterDefinition: {}", graphiteReporterDefinition);
			GraphiteReporter graphiteReporter = graphiteReporterDefinition.buildReporter(this.metricRegistry);
			this.graphiteReporters.add(graphiteReporter);
			graphiteReporter.start(graphiteReporterDefinition.getPeriodDuration(), graphiteReporterDefinition.getPeriodDurationUnit());
		} else {
			LOGGER.warn("unsupported ReporterDefinition: {}: {}", reporterDefinition.getClass(), reporterDefinition);
		}
	}

	@Override
	protected Endpoint createEndpoint(final String uri, final String remaining, final Map<String, Object> parameters) throws Exception {
		LOGGER.debug(MARKER, "createEndpoint({},{},{})", uri, remaining, parameters);
		MetricsEndpoint endpoint = new MetricsEndpoint(uri, this, remaining, parameters);
		setProperties(endpoint, parameters);
		return endpoint;
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
		LOGGER.info(MARKER, "doStart()");
		// calculate merged definitions
		Map<String, ReporterDefinition> leftoverReporterDefinitions = new HashMap<String, ReporterDefinition>();
		leftoverReporterDefinitions.putAll(this.componentReporterDefinitions);
		// merge or add each metric level definition then start it
		for (Entry<String, Map<String, ReporterDefinition>> metricNamedReporterDefinitionsEntry : this.metricReporterDefinitions.entrySet()) {
			String metricName = metricNamedReporterDefinitionsEntry.getKey();
			Map<String, ReporterDefinition> metricReporterDefinitions = metricNamedReporterDefinitionsEntry.getValue();
			for (Entry<String, ReporterDefinition> metricReporterDefinitionEntry : metricReporterDefinitions.entrySet()) {
				String reporterDefinitionName = metricReporterDefinitionEntry.getKey();
				ReporterDefinition metricReporterDefinition = metricReporterDefinitionEntry.getValue();
				ReporterDefinition combinedReporterDefinition = metricReporterDefinition;
				ReporterDefinition componentReporterDefinition = this.componentReporterDefinitions.get(reporterDefinitionName);
				if (componentReporterDefinition != null) {
					// TODO add check to verify definitions are same type
					combinedReporterDefinition = componentReporterDefinition.applyAsOverride(metricReporterDefinition);
				}
				registerAndStart(combinedReporterDefinition);
				// mark definition as finished so we can do delta at end of logic
				leftoverReporterDefinitions.remove(reporterDefinitionName);
			}
		}
		// start the remaining definitions
		for (Entry<String, ReporterDefinition> leftoverReporterDefinitionEntry : leftoverReporterDefinitions.entrySet()) {
			String reporterDefinitionName = leftoverReporterDefinitionEntry.getKey();
			ReporterDefinition reporterDefinition = leftoverReporterDefinitionEntry.getValue();
			registerAndStart(reporterDefinition);
		}

		// TODO start the remaining reporters (graphite,console,etc)
	}

	@Override
	protected void doStop() throws Exception {
		super.doStop();
		LOGGER.debug(MARKER, "doStop()");

	}

	@Override
	protected void doSuspend() throws Exception {
		super.doSuspend();
		LOGGER.debug(MARKER, "doSuspend()");
	}

}
