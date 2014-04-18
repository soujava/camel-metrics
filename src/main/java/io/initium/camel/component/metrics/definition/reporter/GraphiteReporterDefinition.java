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
package io.initium.camel.component.metrics.definition.reporter;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

import io.initium.camel.component.metrics.MetricGroup;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.1
 * @since 2014-02-19
 */
public class GraphiteReporterDefinition extends AbstractReporterDefinition<GraphiteReporterDefinition> {

	// fields
	private static final String		DEFAULT_NAME					= GraphiteReporterDefinition.class.getSimpleName();
	private static final TimeUnit	DEFAULT_DURATION_UNIT			= TimeUnit.MILLISECONDS;
	private static final TimeUnit	DEFAULT_RATE_UNIT				= TimeUnit.SECONDS;
	private static final long		DEFAULT_PERIOD_DURATION			= 1;
	private static final TimeUnit	DEFAULT_PERIOD_DURATION_UNIT	= TimeUnit.MINUTES;
	private static final String		DEFAULT_HOST					= "localhost";
	private static final int		DEFAULT_PORT					= 2003;
	private static final String		DEFAULT_PREFIX					= "metrics";
	private static final String		DEFAULT_RUNTIME_PREFIX			= null;
	private static final String		DEFAULT_RUNTIME_SIMPLE_PREFIX	= null;

	/**
	 * @return
	 */
	public static GraphiteReporterDefinition getDefaultReporter() {
		GraphiteReporterDefinition defaultDefinition = new GraphiteReporterDefinition();
		defaultDefinition.setName(DEFAULT_NAME);
		defaultDefinition.setDurationUnit(DEFAULT_DURATION_UNIT);
		defaultDefinition.setRateUnit(DEFAULT_RATE_UNIT);
		defaultDefinition.setPeriodDuration(DEFAULT_PERIOD_DURATION);
		defaultDefinition.setPeriodDurationUnit(DEFAULT_PERIOD_DURATION_UNIT);
		defaultDefinition.setHost(DEFAULT_HOST);
		defaultDefinition.setPort(DEFAULT_PORT);
		defaultDefinition.setPrefix(DEFAULT_PREFIX);
		defaultDefinition.setRuntimePrefix(DEFAULT_RUNTIME_PREFIX);
		defaultDefinition.setRuntimeSimplePrefix(DEFAULT_RUNTIME_SIMPLE_PREFIX);
		defaultDefinition.setFilter(DEFAULT_FILTER);
		defaultDefinition.setRuntimeFilter(DEFAULT_RUNTIME_FILTER);
		defaultDefinition.setRuntimeSimpleFilter(DEFAULT_RUNTIME_SIMPLE_FILTER);
		return defaultDefinition;
	}

	// fields
	private String		name	= DEFAULT_NAME;
	private TimeUnit	durationUnit;
	private TimeUnit	rateUnit;
	private Long		periodDuration;
	private TimeUnit	periodDurationUnit;
	private String		host;
	private Integer		port;
	private String		prefix;
	private String		runtimePrefix;
	private String		runtimeSimplePrefix;

	@Override
	public GraphiteReporterDefinition applyAsOverride(final GraphiteReporterDefinition override) {
		GraphiteReporterDefinition combinedDefinition = new GraphiteReporterDefinition();
		// get current values
		combinedDefinition.setName(getName());
		combinedDefinition.setDurationUnit(getDurationUnit());
		combinedDefinition.setRateUnit(getRateUnit());
		combinedDefinition.setPeriodDuration(getPeriodDuration());
		combinedDefinition.setPeriodDurationUnit(getPeriodDurationUnit());
		combinedDefinition.setHost(getHost());
		combinedDefinition.setPort(getPort());
		combinedDefinition.setPrefix(getPrefix());
		combinedDefinition.setRuntimePrefix(getRuntimePrefix());
		combinedDefinition.setRuntimeSimplePrefix(getRuntimeSimplePrefix());
		combinedDefinition.setFilter(getFilter());
		combinedDefinition.setRuntimeFilter(getRuntimeFilter());
		combinedDefinition.setRuntimeSimpleFilter(getRuntimeSimpleFilter());
		// apply new values
		combinedDefinition.setNameIfNotNull(override.getName());
		combinedDefinition.setDurationUnitIfNotNull(override.getDurationUnit());
		combinedDefinition.setRateUnitIfNotNull(override.getRateUnit());
		combinedDefinition.setPeriodDurationIfNotNull(override.getPeriodDuration());
		combinedDefinition.setPeriodDurationUnitIfNotNull(override.getPeriodDurationUnit());
		combinedDefinition.setHostIfNotNull(override.getHost());
		combinedDefinition.setPortIfNotNull(override.getPort());
		combinedDefinition.setPrefixIfNotNull(override.getPrefix());
		combinedDefinition.setRuntimePrefixIfNotNull(override.getRuntimePrefix());
		combinedDefinition.setRuntimeSimplePrefixIfNotNull(override.getRuntimeSimplePrefix());
		combinedDefinition.setFilterIfNotNull(override.getFilter());
		combinedDefinition.setRuntimeFilterIfNotNull(override.getRuntimeFilter());
		combinedDefinition.setRuntimeSimpleFilterIfNotNull(override.getRuntimeSimpleFilter());
		return combinedDefinition;
	}

	/**
	 * @param metricRegistry
	 * @return
	 */
	public GraphiteReporter buildReporter(final MetricRegistry metricRegistry, final Exchange creatingExchange, final MetricGroup metricGroup) {
		GraphiteReporterDefinition definitionWithDefaults = getReporterDefinitionWithDefaults();

		final Graphite graphite = new Graphite(new InetSocketAddress(definitionWithDefaults.getHost(), definitionWithDefaults.getPort()));
		final String prefixValue = evaluateValue(definitionWithDefaults.getPrefix(), definitionWithDefaults.getRuntimePrefix(), definitionWithDefaults.getRuntimeSimplePrefix(), creatingExchange);
		final String filterValue = evaluateValue(definitionWithDefaults.getFilter(), definitionWithDefaults.getRuntimeFilter(), definitionWithDefaults.getRuntimeSimpleFilter(), creatingExchange);

		// @formatter:off
		GraphiteReporter graphiteReporter = GraphiteReporter
				.forRegistry(metricRegistry)
				.prefixedWith(prefixValue)
				.convertDurationsTo(definitionWithDefaults.getDurationUnit())
				.convertRatesTo(definitionWithDefaults.getRateUnit())
				.filter(new MetricFilter(){
					@Override
					public boolean matches(final String name, final Metric metric) {
						if(!metricGroup.contains(metric)){
							return false;
						}
						if(name==null || filterValue==null){
							return true;
						}
						boolean result = name.matches(filterValue);
						return result;
					}
				})
				.build(graphite);
		// @formatter:on
		return graphiteReporter;
	}

	/**
	 * @return the durationUnit
	 */
	public TimeUnit getDurationUnit() {
		return this.durationUnit;
	}

	/**
	 * @return the host
	 */
	public String getHost() {
		return this.host;
	}

	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * @return the periodDuration
	 */
	public Long getPeriodDuration() {
		return this.periodDuration;
	}

	/**
	 * @return the periodDurationUnit
	 */
	public TimeUnit getPeriodDurationUnit() {
		return this.periodDurationUnit;
	}

	/**
	 * @return the port
	 */
	public Integer getPort() {
		return this.port;
	}

	public String getPrefix() {
		return this.prefix;
	}

	/**
	 * @return the rateUnit
	 */
	public TimeUnit getRateUnit() {
		return this.rateUnit;
	}

	@Override
	public GraphiteReporterDefinition getReporterDefinitionWithDefaults() {
		return getDefaultReporter().applyAsOverride(this);
	}

	/**
	 * @return the runtimePrefix
	 */
	public String getRuntimePrefix() {
		return this.runtimePrefix;
	}

	/**
	 * @return the runtimeSimplePrefix
	 */
	public String getRuntimeSimplePrefix() {
		return this.runtimeSimplePrefix;
	}

	@Override
	public void setDurationUnit(final TimeUnit durationUnit) {
		this.durationUnit = durationUnit;
	}

	/**
	 * @param host
	 *            the host to set
	 */
	public void setHost(final String host) {
		this.host = host;
	}

	@Override
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * @param periodDurationInt
	 *            the periodDuration to set
	 */
	public void setPeriodDuration(final Integer periodDurationInt) {
		this.periodDuration = periodDurationInt.longValue();
	}

	/**
	 * @param periodDuration
	 *            the periodDuration to set
	 */
	public void setPeriodDuration(final Long periodDuration) {
		this.periodDuration = periodDuration;
	}

	/**
	 * @param periodDurationUnit
	 *            the periodDurationUnit to set
	 */
	public void setPeriodDurationUnit(final TimeUnit periodDurationUnit) {
		this.periodDurationUnit = periodDurationUnit;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public void setPort(final Integer port) {
		this.port = port;
	}

	public void setPrefix(final String prefix) {
		this.prefix = prefix;
	}

	public void setPrefixIfNotNull(final String prefix) {
		if (prefix != null) {
			setPrefix(prefix);
		}
	}

	@Override
	public void setRateUnit(final TimeUnit rateUnit) {
		this.rateUnit = rateUnit;
	}

	/**
	 * @param runtimePrefix
	 *            the runtimePrefix to set
	 */
	public void setRuntimePrefix(final String runtimePrefix) {
		this.runtimePrefix = runtimePrefix;
	}

	/**
	 * @param runtimePrefix
	 *            the runtimePrefix to set
	 */
	public void setRuntimePrefixIfNotNull(final String runtimePrefix) {
		if (runtimePrefix != null) {
			setRuntimePrefix(runtimePrefix);
		}
	}

	/**
	 * @param runtimeSimplePrefix
	 *            the runtimeSimplePrefix to set
	 */
	public void setRuntimeSimplePrefix(final String runtimeSimplePrefix) {
		this.runtimeSimplePrefix = runtimeSimplePrefix;
	}

	/**
	 * @param runtimeSimplePrefix
	 *            the runtimeSimplePrefix to set
	 */
	public void setRuntimeSimplePrefixIfNotNull(final String runtimeSimplePrefix) {
		if (runtimeSimplePrefix != null) {
			setRuntimePrefix(runtimeSimplePrefix);
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("GraphiteReporterDefinition [name=");
		builder.append(this.name);
		builder.append(", durationUnit=");
		builder.append(this.durationUnit);
		builder.append(", rateUnit=");
		builder.append(this.rateUnit);
		builder.append(", periodDuration=");
		builder.append(this.periodDuration);
		builder.append(", periodDurationUnit=");
		builder.append(this.periodDurationUnit);
		builder.append(", host=");
		builder.append(this.host);
		builder.append(", port=");
		builder.append(this.port);
		builder.append(", prefix=");
		builder.append(this.prefix);
		builder.append(", runtimePrefix=");
		builder.append(this.runtimePrefix);
		builder.append(", runtimeSimplePrefix=");
		builder.append(this.runtimeSimplePrefix);
		builder.append(", getFilter()=");
		builder.append(getFilter());
		builder.append(", getRuntimeFilter()=");
		builder.append(getRuntimeFilter());
		builder.append(", getRuntimeSimpleFilter()=");
		builder.append(getRuntimeSimpleFilter());
		builder.append("]");
		return builder.toString();
	}

	/**
	 * @param durationUnit
	 */
	private void setDurationUnitIfNotNull(final TimeUnit durationUnit) {
		if (durationUnit != null) {
			setDurationUnit(durationUnit);
		}
	}

	private void setHostIfNotNull(final String host) {
		if (host != null) {
			setHost(host);
		}
	}

	/**
	 * @param name
	 */
	private void setNameIfNotNull(final String name) {
		if (name != null) {
			setName(name);
		}
	}

	private void setPeriodDurationIfNotNull(final Long periodDuration) {
		if (periodDuration != null) {
			setPeriodDuration(periodDuration);
		}
	}

	private void setPeriodDurationUnitIfNotNull(final TimeUnit periodDurationUnit) {
		if (periodDurationUnit != null) {
			setPeriodDurationUnit(periodDurationUnit);
		}
	}

	private void setPortIfNotNull(final Integer port) {
		if (port != null) {
			setPort(port);
		}
	}

	/**
	 * @param rateUnit
	 */
	private void setRateUnitIfNotNull(final TimeUnit rateUnit) {
		if (rateUnit != null) {
			setRateUnit(rateUnit);
		}
	}
}
