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

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.1
 * @since 2014-02-19
 */
public class ConsoleReporterDefinition implements ReporterDefinition<ConsoleReporterDefinition> {

	// fields
	private static final String		DEFAULT_NAME					= ConsoleReporterDefinition.class.getSimpleName();
	private static final TimeUnit	DEFAULT_DURATION_UNIT			= TimeUnit.MILLISECONDS;
	private static final TimeUnit	DEFAULT_RATE_UNIT				= TimeUnit.SECONDS;
	private static final long		DEFAULT_PERIOD_DURATION			= 1;
	private static final TimeUnit	DEFAULT_PERIOD_DURATION_UNIT	= TimeUnit.MINUTES;
	private static final String		DEFAULT_FILTER					= null;

	/**
	 * @return
	 */
	public static ConsoleReporterDefinition getDefaultReporter() {
		ConsoleReporterDefinition consoleReporterDefinition = new ConsoleReporterDefinition();
		consoleReporterDefinition.setName(DEFAULT_NAME);
		consoleReporterDefinition.setDurationUnit(DEFAULT_DURATION_UNIT);
		consoleReporterDefinition.setRateUnit(DEFAULT_RATE_UNIT);
		consoleReporterDefinition.setPeriodDuration(DEFAULT_PERIOD_DURATION);
		consoleReporterDefinition.setPeriodDurationUnit(DEFAULT_PERIOD_DURATION_UNIT);
		consoleReporterDefinition.setFilter(DEFAULT_FILTER);
		return consoleReporterDefinition;
	}

	// fields
	private String		name	= DEFAULT_NAME;
	private TimeUnit	durationUnit;
	private TimeUnit	rateUnit;
	private Long		periodDuration;
	private TimeUnit	periodDurationUnit;
	private String		filter;

	@Override
	public ConsoleReporterDefinition applyAsOverride(final ConsoleReporterDefinition override) {
		ConsoleReporterDefinition consoleReporterDefinition = new ConsoleReporterDefinition();
		// get current values
		consoleReporterDefinition.setName(this.name);
		consoleReporterDefinition.setDurationUnit(this.durationUnit);
		consoleReporterDefinition.setRateUnit(this.rateUnit);
		consoleReporterDefinition.setPeriodDuration(this.periodDuration);
		consoleReporterDefinition.setPeriodDurationUnit(this.periodDurationUnit);
		consoleReporterDefinition.setFilter(this.filter);
		// apply new values
		consoleReporterDefinition.setNameIfNotNull(override.getName());
		consoleReporterDefinition.setDurationUnitIfNotNull(override.getDurationUnit());
		consoleReporterDefinition.setRateUnitIfNotNull(override.getDurationUnit());
		consoleReporterDefinition.setPeriodDurationIfNotNull(override.getPeriodDuration());
		consoleReporterDefinition.setPeriodDurationUnitIfNotNull(override.getPeriodDurationUnit());
		consoleReporterDefinition.setFilterIfNotNull(override.getFilter());
		return consoleReporterDefinition;
	}

	/**
	 * @param metricRegistry
	 * @return
	 */
	public ConsoleReporter buildReporter(final MetricRegistry metricRegistry) {
		ConsoleReporterDefinition consoleReporterDefinition = getReporterDefinitionWithDefaults();
		// @formatter:off
		ConsoleReporter consoleReporter = ConsoleReporter
				.forRegistry(metricRegistry)
				.convertDurationsTo(consoleReporterDefinition.getDurationUnit())
				.convertRatesTo(consoleReporterDefinition.getRateUnit())
				.filter(new MetricFilter(){
					@Override
					public boolean matches(final String name, final Metric metric) {
						if(name==null || ConsoleReporterDefinition.this.filter==null){
							return true;
						}
						boolean result = name.matches(ConsoleReporterDefinition.this.filter);
						return result;
					}
				})
				.build();
		// @formatter:on
		return consoleReporter;
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
	 * @return the rateUnit
	 */
	public TimeUnit getRateUnit() {
		return this.rateUnit;
	}

	@Override
	public ConsoleReporterDefinition getReporterDefinitionWithDefaults() {
		return getDefaultReporter().applyAsOverride(this);
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

	@Override
	public void setRateUnit(final TimeUnit rateUnit) {
		this.rateUnit = rateUnit;
	}

	@Override
	public String toString() {
		return "ConsoleReporterDefinition [name=" + this.name + ", durationUnit=" + this.durationUnit + ", rateUnit=" + this.rateUnit + ", periodDuration=" + this.periodDuration + ", periodDurationUnit=" + this.periodDurationUnit + "]";
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

	/**
	 * @param rateUnit
	 */
	private void setRateUnitIfNotNull(final TimeUnit rateUnit) {
		if (rateUnit != null) {
			setRateUnit(rateUnit);
		}
	}

}
