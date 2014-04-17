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
	private static final String		DEFAULT_DYNAMIC_PREFIX			= null;
	private static final String		DEFAULT_FILTER					= null;
	private static final String		DEFAULT_DYNAMIC_FILTER			= null;

	/**
	 * @return
	 */
	public static GraphiteReporterDefinition getDefaultReporter() {
		GraphiteReporterDefinition graphiteReporterDefinition = new GraphiteReporterDefinition();
		graphiteReporterDefinition.setName(DEFAULT_NAME);
		graphiteReporterDefinition.setDurationUnit(DEFAULT_DURATION_UNIT);
		graphiteReporterDefinition.setRateUnit(DEFAULT_RATE_UNIT);
		graphiteReporterDefinition.setPeriodDuration(DEFAULT_PERIOD_DURATION);
		graphiteReporterDefinition.setPeriodDurationUnit(DEFAULT_PERIOD_DURATION_UNIT);
		graphiteReporterDefinition.setHost(DEFAULT_HOST);
		graphiteReporterDefinition.setPort(DEFAULT_PORT);
		graphiteReporterDefinition.setPrefix(DEFAULT_PREFIX);
		graphiteReporterDefinition.setDynamicPrefix(DEFAULT_DYNAMIC_PREFIX);
		graphiteReporterDefinition.setFilter(DEFAULT_FILTER);
		graphiteReporterDefinition.setDynamicFilter(DEFAULT_DYNAMIC_FILTER);
		return graphiteReporterDefinition;
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
	private String		dynamicPrefix;
	private String		filter;

	private String		dynamicFilter;

	@Override
	public GraphiteReporterDefinition applyAsOverride(final GraphiteReporterDefinition override) {
		GraphiteReporterDefinition graphiteReporterDefinition = new GraphiteReporterDefinition();
		// get current values
		graphiteReporterDefinition.setName(this.name);
		graphiteReporterDefinition.setDurationUnit(this.durationUnit);
		graphiteReporterDefinition.setRateUnit(this.rateUnit);
		graphiteReporterDefinition.setPeriodDuration(this.periodDuration);
		graphiteReporterDefinition.setPeriodDurationUnit(this.periodDurationUnit);
		graphiteReporterDefinition.setHost(this.host);
		graphiteReporterDefinition.setPort(this.port);
		graphiteReporterDefinition.setPrefix(this.prefix);
		graphiteReporterDefinition.setDynamicPrefix(this.dynamicPrefix);
		graphiteReporterDefinition.setFilter(this.filter);
		graphiteReporterDefinition.setDynamicFilter(this.dynamicFilter);
		// apply new values
		graphiteReporterDefinition.setNameIfNotNull(override.getName());
		graphiteReporterDefinition.setDurationUnitIfNotNull(override.getDurationUnit());
		graphiteReporterDefinition.setRateUnitIfNotNull(override.getRateUnit());
		graphiteReporterDefinition.setPeriodDurationIfNotNull(override.getPeriodDuration());
		graphiteReporterDefinition.setPeriodDurationUnitIfNotNull(override.getPeriodDurationUnit());
		graphiteReporterDefinition.setHostIfNotNull(override.getHost());
		graphiteReporterDefinition.setPortIfNotNull(override.getPort());
		graphiteReporterDefinition.setPrefixIfNotNull(override.getPrefix());
		graphiteReporterDefinition.setDynamicPrefixIfNotNull(override.getDynamicPrefix());
		graphiteReporterDefinition.setFilterIfNotNull(override.getFilter());
		graphiteReporterDefinition.setDynamicFilterIfNotNull(override.getDynamicFilter());
		return graphiteReporterDefinition;
	}

	/**
	 * @param metricRegistry
	 * @return
	 */
	public GraphiteReporter buildReporter(final MetricRegistry metricRegistry, final Exchange creatingExchange, final MetricGroup metricGroup) {
		GraphiteReporterDefinition graphiteReporterDefinition = getReporterDefinitionWithDefaults();
		final Graphite graphite = new Graphite(new InetSocketAddress(graphiteReporterDefinition.getHost(), graphiteReporterDefinition.getPort()));

		final String evaluatedFilter = creatingExchange == null ? this.filter : evaluateExpression(graphiteReporterDefinition.getDynamicFilter(), creatingExchange, String.class);
		final String evaluatedPrefix = creatingExchange == null ? this.prefix : evaluateExpression(graphiteReporterDefinition.getDynamicPrefix(), creatingExchange, String.class);

		// @formatter:off
		GraphiteReporter graphiteReporter = GraphiteReporter
				.forRegistry(metricRegistry)
				.prefixedWith(evaluatedPrefix)
				.convertDurationsTo(graphiteReporterDefinition.getDurationUnit())
				.convertRatesTo(graphiteReporterDefinition.getRateUnit())
				.filter(new MetricFilter(){
					@Override
					public boolean matches(final String name, final Metric metric) {
						if(!metricGroup.contains(metric)){
							return false;
						}
						if(name==null || evaluatedFilter==null){
							return true;
						}
						boolean result = name.matches(evaluatedFilter);
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
	 * @return the dynamicFilter
	 */
	public String getDynamicFilter() {
		return this.dynamicFilter;
	}

	/**
	 * @return the dynamicPrefix
	 */
	public String getDynamicPrefix() {
		return this.dynamicPrefix;
	}

	/**
	 * @return the filter
	 */
	public String getFilter() {
		return this.filter;
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

	@Override
	public void setDurationUnit(final TimeUnit durationUnit) {
		this.durationUnit = durationUnit;
	}

	/**
	 * @param dynamicFilter
	 *            the dynamicFilter to set
	 */
	public void setDynamicFilter(final String dynamicFilter) {
		this.dynamicFilter = dynamicFilter;
	}

	/**
	 * @param dynamicFilter
	 *            the dynamicFilter to set
	 */
	public void setDynamicFilterIfNotNull(final String dynamicFilter) {
		if (dynamicFilter != null) {
			setDynamicFilter(dynamicFilter);
		}
	}

	/**
	 * @param dynamicPrefix
	 *            the dynamicPrefix to set
	 */
	public void setDynamicPrefix(final String dynamicPrefix) {
		this.dynamicPrefix = dynamicPrefix;
	}

	/**
	 * @param dynamicPrefix
	 *            the dynamicPrefix to set
	 */
	public void setDynamicPrefixIfNotNull(final String dynamicPrefix) {
		if (dynamicPrefix != null) {
			setDynamicPrefix(dynamicPrefix);
		}
	}

	/**
	 * @param filter
	 *            the filter to set
	 */
	public void setFilter(final String filter) {
		this.filter = filter;
	}

	/**
	 * @param filter
	 *            the filter to set
	 */
	public void setFilterIfNotNull(final String filter) {
		if (filter != null) {
			setFilter(filter);
		}
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

	@Override
	public String toString() {
		return "GraphiteReporterDefinition [name=" + this.name + ", durationUnit=" + this.durationUnit + ", rateUnit=" + this.rateUnit + ", periodDuration=" + this.periodDuration + ", periodDurationUnit=" + this.periodDurationUnit + ", host=" + this.host
				+ ", port=" + this.port + ", prefix=" + this.prefix + ", dynamicPrefix=" + this.dynamicPrefix + ", filter=" + this.filter + ", dynamicFilter=" + this.dynamicFilter + "]";
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