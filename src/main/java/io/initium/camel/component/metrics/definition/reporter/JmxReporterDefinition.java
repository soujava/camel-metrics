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

import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;

import io.initium.camel.component.metrics.MetricGroup;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.1
 * @since 2014-02-19
 */
public class JmxReporterDefinition extends AbstractReporterDefinition<JmxReporterDefinition> {

	// fields
	private static final String		DEFAULT_NAME					= JmxReporterDefinition.class.getSimpleName();
	private static final TimeUnit	DEFAULT_DURATION_UNIT			= TimeUnit.MILLISECONDS;
	private static final TimeUnit	DEFAULT_RATE_UNIT				= TimeUnit.SECONDS;
	private static final String		DEFAULT_DOMAIN					= "metrics";
	private static final String		DEFAULT_RUNTIME_DOMAIN			= null;
	private static final String		DEFAULT_RUNTIME_SIMPLE_DOMAIN	= null;

	/**
	 * @return
	 */
	public static JmxReporterDefinition getDefaultReporter() {
		JmxReporterDefinition defaultDefinition = new JmxReporterDefinition();
		defaultDefinition.setName(DEFAULT_NAME);
		defaultDefinition.setDurationUnit(DEFAULT_DURATION_UNIT);
		defaultDefinition.setRateUnit(DEFAULT_RATE_UNIT);
		defaultDefinition.setDomain(DEFAULT_DOMAIN);
		defaultDefinition.setRuntimeDomain(DEFAULT_RUNTIME_DOMAIN);
		defaultDefinition.setRuntimeSimpleDomain(DEFAULT_RUNTIME_SIMPLE_DOMAIN);
		defaultDefinition.setFilter(DEFAULT_FILTER);
		defaultDefinition.setRuntimeFilter(DEFAULT_RUNTIME_FILTER);
		defaultDefinition.setRuntimeSimpleFilter(DEFAULT_RUNTIME_SIMPLE_FILTER);
		return defaultDefinition;
	}

	// fields
	private String		name	= DEFAULT_NAME;
	private TimeUnit	durationUnit;
	private TimeUnit	rateUnit;
	private String		domain;
	private String		runtimeDomain;
	private String		runtimeSimpleDomain;

	@Override
	public JmxReporterDefinition applyAsOverride(final JmxReporterDefinition override) {
		JmxReporterDefinition combinedDefinition = new JmxReporterDefinition();
		// get current values
		combinedDefinition.setName(getName());
		combinedDefinition.setDurationUnit(getDurationUnit());
		combinedDefinition.setRateUnit(getRateUnit());
		combinedDefinition.setDomain(getDomain());
		combinedDefinition.setRuntimeDomain(getRuntimeDomain());
		combinedDefinition.setRuntimeSimpleDomain(getRuntimeSimpleDomain());
		combinedDefinition.setFilter(getFilter());
		combinedDefinition.setRuntimeFilter(getRuntimeFilter());
		combinedDefinition.setRuntimeSimpleFilter(getRuntimeSimpleFilter());
		// apply new values
		combinedDefinition.setNameIfNotNull(override.getName());
		combinedDefinition.setDurationUnitIfNotNull(override.getDurationUnit());
		combinedDefinition.setRateUnitIfNotNull(override.getRateUnit());
		combinedDefinition.setDomainIfNotNull(override.getDomain());
		combinedDefinition.setRuntimeDomainIfNotNull(override.getRuntimeDomain());
		combinedDefinition.setRuntimeSimpleDomainIfNotNull(override.getRuntimeSimpleDomain());
		combinedDefinition.setFilterIfNotNull(override.getFilter());
		combinedDefinition.setRuntimeFilterIfNotNull(override.getRuntimeFilter());
		combinedDefinition.setRuntimeSimpleFilterIfNotNull(override.getRuntimeSimpleFilter());
		return combinedDefinition;
	}

	/**
	 * @param metricRegistry
	 * @param creatingExchange
	 * @return
	 */
	public JmxReporter buildReporter(final MetricRegistry metricRegistry, final Exchange creatingExchange, final MetricGroup metricGroup) {
		JmxReporterDefinition definitionWithDefaults = getReporterDefinitionWithDefaults();

		final String domainValue = evaluateValue(definitionWithDefaults.getDomain(), definitionWithDefaults.getRuntimeDomain(), definitionWithDefaults.getRuntimeSimpleDomain(), creatingExchange);
		final String filterValue = evaluateValue(definitionWithDefaults.getFilter(), definitionWithDefaults.getRuntimeFilter(), definitionWithDefaults.getRuntimeSimpleFilter(), creatingExchange);

		// @formatter:off
		JmxReporter jmxReporter = JmxReporter
				.forRegistry(metricRegistry)
				.inDomain(domainValue)
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
				.build();
		// @formatter:on
		return jmxReporter;
	}

	/**
	 * @return the domain
	 */
	public String getDomain() {
		return this.domain;
	}

	/**
	 * @return the durationUnit
	 */
	public TimeUnit getDurationUnit() {
		return this.durationUnit;
	}

	@Override
	public String getName() {
		return this.name;
	}

	/**
	 * @return the rateUnit
	 */
	public TimeUnit getRateUnit() {
		return this.rateUnit;
	}

	@Override
	public JmxReporterDefinition getReporterDefinitionWithDefaults() {
		return getDefaultReporter().applyAsOverride(this);
	}

	/**
	 * @return the runtimeDomain
	 */
	public String getRuntimeDomain() {
		return this.runtimeDomain;
	}

	/**
	 * @return the runtimeSimpleDomain
	 */
	public String getRuntimeSimpleDomain() {
		return this.runtimeSimpleDomain;
	}

	/**
	 * @param domain
	 */
	public void setDomain(final String domain) {
		this.domain = domain;
	}

	@Override
	public void setDurationUnit(final TimeUnit durationUnit) {
		this.durationUnit = durationUnit;
	}

	@Override
	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public void setRateUnit(final TimeUnit rateUnit) {
		this.rateUnit = rateUnit;
	}

	/**
	 * @param runtimeDomain
	 *            the runtimeDomain to set
	 */
	public void setRuntimeDomain(final String runtimeDomain) {
		this.runtimeDomain = runtimeDomain;
	}

	/**
	 * @param runtimeDomain
	 *            the runtimeDomain to set
	 */
	public void setRuntimeDomainIfNotNull(final String runtimeDomain) {
		if (runtimeDomain != null) {
			setRuntimeDomain(runtimeDomain);
		}
	}

	/**
	 * @param runtimeSimpleDomain
	 *            the runtimeSimpleDomain to set
	 */
	public void setRuntimeSimpleDomain(final String runtimeSimpleDomain) {
		this.runtimeSimpleDomain = runtimeSimpleDomain;
	}

	/**
	 * @param runtimeSimpleDomain
	 *            the runtimeSimpleDomain to set
	 */
	public void setRuntimeSimpleDomainIfNotNull(final String runtimeSimpleDomain) {
		if (runtimeSimpleDomain != null) {
			setRuntimeSimpleDomain(runtimeSimpleDomain);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("JmxReporterDefinition [name=");
		builder.append(this.name);
		builder.append(", durationUnit=");
		builder.append(this.durationUnit);
		builder.append(", rateUnit=");
		builder.append(this.rateUnit);
		builder.append(", domain=");
		builder.append(this.domain);
		builder.append(", runtimeDomain=");
		builder.append(this.runtimeDomain);
		builder.append(", runtimeSimpleDomain=");
		builder.append(this.runtimeSimpleDomain);
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
	 * @param domain
	 */
	private void setDomainIfNotNull(final String domain) {
		if (domain != null) {
			setDomain(domain);
		}
	}

	/**
	 * @param durationUnit
	 */
	private void setDurationUnitIfNotNull(final TimeUnit durationUnit) {
		if (durationUnit != null) {
			setDurationUnit(durationUnit);
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

	/**
	 * @param rateUnit
	 */
	private void setRateUnitIfNotNull(final TimeUnit rateUnit) {
		if (rateUnit != null) {
			setRateUnit(rateUnit);
		}
	}

}
