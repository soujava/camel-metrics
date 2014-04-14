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
package io.initium.camel.component.metrics.reporters;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.1
 * @since 2014-02-19
 */
public class JmxReporterDefinition implements ReporterDefinition<JmxReporterDefinition> {

	// fields
	private static final String		DEFAULT_NAME			= JmxReporterDefinition.class.getSimpleName();
	private static final String		DEFAULT_DOMAIN			= "metrics";
	private static final TimeUnit	DEFAULT_DURATION_UNIT	= TimeUnit.MILLISECONDS;
	private static final TimeUnit	DEFAULT_RATE_UNIT		= TimeUnit.SECONDS;
	private static final String		DEFAULT_FILTER			= null;

	/**
	 * @return
	 */
	public static JmxReporterDefinition getDefaultReporter() {
		JmxReporterDefinition jmxReporterDefinition = new JmxReporterDefinition();
		jmxReporterDefinition.setName(DEFAULT_NAME);
		jmxReporterDefinition.setDomain(DEFAULT_DOMAIN);
		jmxReporterDefinition.setDurationUnit(DEFAULT_DURATION_UNIT);
		jmxReporterDefinition.setRateUnit(DEFAULT_RATE_UNIT);
		jmxReporterDefinition.setFilter(DEFAULT_FILTER);
		return jmxReporterDefinition;
	}

	// fields
	private String		name	= DEFAULT_NAME;
	private String		domain;
	private TimeUnit	durationUnit;
	private TimeUnit	rateUnit;
	private String		filter;

	@Override
	public JmxReporterDefinition applyAsOverride(final JmxReporterDefinition override) {
		JmxReporterDefinition jmxReporterDefinition = new JmxReporterDefinition();
		// get current values
		jmxReporterDefinition.setName(this.name);
		jmxReporterDefinition.setDomain(this.domain);
		jmxReporterDefinition.setDurationUnit(this.durationUnit);
		jmxReporterDefinition.setRateUnit(this.rateUnit);
		jmxReporterDefinition.setFilter(this.filter);
		// apply new values
		jmxReporterDefinition.setNameIfNotNull(override.getName());
		jmxReporterDefinition.setDomainIfNotNull(override.getDomain());
		jmxReporterDefinition.setDurationUnitIfNotNull(override.getDurationUnit());
		jmxReporterDefinition.setRateUnitIfNotNull(override.getDurationUnit());
		jmxReporterDefinition.setFilterIfNotNull(override.getFilter());
		return jmxReporterDefinition;
	}

	/**
	 * @param metricRegistry
	 * @return
	 */
	public JmxReporter buildReporter(final MetricRegistry metricRegistry) {
		JmxReporterDefinition jmxReporterDefinition = getReporterDefinitionWithDefaults();
		// @formatter:off
		JmxReporter jmxReporter = JmxReporter
				.forRegistry(metricRegistry)
				.inDomain(jmxReporterDefinition.getDomain())
				.convertDurationsTo(jmxReporterDefinition.getDurationUnit())
				.convertRatesTo(jmxReporterDefinition.getRateUnit())
				.filter(new MetricFilter(){
					@Override
					public boolean matches(final String name, final Metric metric) {
						if(name==null || JmxReporterDefinition.this.filter==null){
							return true;
						}
						boolean result = name.matches(JmxReporterDefinition.this.filter);
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

	/**
	 * @return the filter
	 */
	public String getFilter() {
		return this.filter;
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
	 * @param domain
	 */
	public void setDomain(final String domain) {
		this.domain = domain;
	}

	@Override
	public void setDurationUnit(final TimeUnit durationUnit) {
		this.durationUnit = durationUnit;
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

	@Override
	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public void setRateUnit(final TimeUnit rateUnit) {
		this.rateUnit = rateUnit;
	}

	@Override
	public String toString() {
		return "JmxReporterDefinition [name=" + this.name + ", domain=" + this.domain + ", durationUnit=" + this.durationUnit + ", rateUnit=" + this.rateUnit + "]";
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
