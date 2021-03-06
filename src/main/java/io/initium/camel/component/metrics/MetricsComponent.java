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
import java.util.Set;

import org.apache.camel.Endpoint;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.impl.UriEndpointComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.codahale.metrics.MetricRegistry;

import io.initium.camel.component.metrics.definition.reporter.ReporterDefinition;
import io.initium.camel.component.metrics.listener.LoggingMetricRegistryListener;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.0
 * @since 2014-02-19
 */
@SuppressWarnings("rawtypes")
public class MetricsComponent extends UriEndpointComponent {

	// TODO remove suppress "rawtypes" warnings by refactoring ReporterDefinition

	// logging
	private static final String						SELF				= Thread.currentThread().getStackTrace()[1].getClassName();
	private static final Logger						LOGGER				= LoggerFactory.getLogger(SELF);

	// constants
	public static final Marker						MARKER				= MarkerFactory.getMarker("metrics");
	public static final String						DEFAULT_JMX_DOMAIN	= "metrics";
	public static final String						TIMING_MAP_NAME		= DEFAULT_JMX_DOMAIN + ".TimingMap";

	// fields
	private final Map<String, ReporterDefinition>	reporterDefinitions	= new HashMap<String, ReporterDefinition>();
	private final Set<String>						metricNames			= new HashSet<String>();
	private final MetricRegistry					metricRegistry;
	private final Map<String, MetricGroup>			metricGroups		= new HashMap<String, MetricGroup>();

	/**
	 * 
	 */
	public MetricsComponent() {
		super(MetricsEndpoint.class);
		LOGGER.info(MARKER, "MetricsComponent()");
		this.metricRegistry = new MetricRegistry();
		LoggingMetricRegistryListener listener = new LoggingMetricRegistryListener(LOGGER, MARKER, LoggingMetricRegistryListener.Level.INFO);
		this.metricRegistry.addListener(listener);
	}

	/**
	 * @param newReporterDefinitions
	 */
	public MetricsComponent(final ReporterDefinition... newReporterDefinitions) {
		this();
		LOGGER.info(MARKER, "MetricsComponent({})", (Object[]) newReporterDefinitions);
		for (ReporterDefinition reporterDefinition : newReporterDefinitions) {
			String reporterDefinitionName = reporterDefinition.getName();
			if (reporterDefinitionName != null && this.reporterDefinitions.containsKey(reporterDefinitionName)) {
				throw new RuntimeCamelException("duplicate ReporterDefinition encountered: " + reporterDefinitionName);
			}
			this.reporterDefinitions.put(reporterDefinitionName, reporterDefinition);
		}
	}

	public Map<String, MetricGroup> getMetricGroups() {
		return this.metricGroups;
	}

	public MetricRegistry getMetricRegistry() {
		return this.metricRegistry;
	}

	/**
	 * @return the reporterDefinitions
	 */
	public Map<String, ReporterDefinition> getReporterDefinitions() {
		return this.reporterDefinitions;
	}

	/**
	 * @param name
	 */
	public synchronized void registerName(final String name) {
		if (this.metricNames.contains(name)) {
			throw new RuntimeCamelException("duplicate metric name found: " + name);
		}
		this.metricNames.add(name);
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
