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

import com.codahale.metrics.ConsoleReporter;
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
public class ConsoleReporterDefinition extends AbstractReporterDefinition<ConsoleReporterDefinition> {

	// fields
	private static final String		DEFAULT_NAME					= ConsoleReporterDefinition.class.getSimpleName();
	private static final TimeUnit	DEFAULT_DURATION_UNIT			= TimeUnit.MILLISECONDS;
	private static final TimeUnit	DEFAULT_RATE_UNIT				= TimeUnit.SECONDS;
	private static final long		DEFAULT_PERIOD_DURATION			= 1;
	private static final TimeUnit	DEFAULT_PERIOD_DURATION_UNIT	= TimeUnit.MINUTES;

	/**
	 * @return
	 */
	public static ConsoleReporterDefinition getDefaultReporter() {
		ConsoleReporterDefinition defaultDefinition = new ConsoleReporterDefinition();
		defaultDefinition.setName(DEFAULT_NAME);
		defaultDefinition.setDurationUnit(DEFAULT_DURATION_UNIT);
		defaultDefinition.setRateUnit(DEFAULT_RATE_UNIT);
		defaultDefinition.setPeriodDuration(DEFAULT_PERIOD_DURATION);
		defaultDefinition.setPeriodDurationUnit(DEFAULT_PERIOD_DURATION_UNIT);
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

	@Override
	public ConsoleReporterDefinition applyAsOverride(final ConsoleReporterDefinition override) {
		ConsoleReporterDefinition combinedDefinition = new ConsoleReporterDefinition();
		// get current values
		combinedDefinition.setName(getName());
		combinedDefinition.setDurationUnit(getDurationUnit());
		combinedDefinition.setRateUnit(getRateUnit());
		combinedDefinition.setPeriodDuration(getPeriodDuration());
		combinedDefinition.setPeriodDurationUnit(this.periodDurationUnit);
		combinedDefinition.setFilter(getFilter());
		combinedDefinition.setRuntimeFilter(getRuntimeFilter());
		combinedDefinition.setRuntimeSimpleFilter(getRuntimeSimpleFilter());
		// apply new values
		combinedDefinition.setNameIfNotNull(override.getName());
		combinedDefinition.setDurationUnitIfNotNull(override.getDurationUnit());
		combinedDefinition.setRateUnitIfNotNull(override.getRateUnit());
		combinedDefinition.setPeriodDurationIfNotNull(override.getPeriodDuration());
		combinedDefinition.setPeriodDurationUnitIfNotNull(override.getPeriodDurationUnit());
		combinedDefinition.setFilterIfNotNull(override.getFilter());
		combinedDefinition.setRuntimeFilterIfNotNull(override.getRuntimeFilter());
		combinedDefinition.setRuntimeSimpleFilterIfNotNull(override.getRuntimeSimpleFilter());
		return combinedDefinition;
	}

	/**
	 * @param metricRegistry
	 * @return
	 */
	public ConsoleReporter buildReporter(final MetricRegistry metricRegistry, final Exchange creatingExchange, final MetricGroup metricGroup) {
		ConsoleReporterDefinition definitionWithDefaults = getReporterDefinitionWithDefaults();

		final String filterValue = evaluateValue(definitionWithDefaults.getFilter(), definitionWithDefaults.getRuntimeFilter(), definitionWithDefaults.getRuntimeSimpleFilter(), creatingExchange);

		// @formatter:off
		ConsoleReporter consoleReporter = ConsoleReporter
				.forRegistry(metricRegistry)
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
		return consoleReporter;
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
		StringBuilder builder = new StringBuilder();
		builder.append("ConsoleReporterDefinition [name=");
		builder.append(this.name);
		builder.append(", durationUnit=");
		builder.append(this.durationUnit);
		builder.append(", rateUnit=");
		builder.append(this.rateUnit);
		builder.append(", periodDuration=");
		builder.append(this.periodDuration);
		builder.append(", periodDurationUnit=");
		builder.append(this.periodDurationUnit);
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
