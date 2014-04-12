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

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.builder.ToStringBuilder;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.1
 * @since 2014-02-19
 */
public class ConsoleReporterDefinition implements ReporterDefinition<ConsoleReporterDefinition> {

	// fields
	private static final TimeUnit	DEFAULT_DURATION_UNIT			= TimeUnit.MILLISECONDS;
	private static final TimeUnit	DEFAULT_RATE_UNIT				= TimeUnit.SECONDS;
	private static final long		DEFAULT_PERIOD_DURATION			= 1;
	private static final TimeUnit	DEFAULT_PERIOD_DURATION_UNIT	= TimeUnit.MINUTES;

	/**
	 * @return
	 */
	public static ConsoleReporterDefinition getDefaultReporter() {
		return getDefaultReporter(UUID.randomUUID().toString());
	}

	/**
	 * @param name
	 * @return
	 */
	public static ConsoleReporterDefinition getDefaultReporter(final String name) {
		ConsoleReporterDefinition consoleReporterDefinition = new ConsoleReporterDefinition();
		consoleReporterDefinition.setName(name);
		consoleReporterDefinition.setDurationUnit(DEFAULT_DURATION_UNIT);
		consoleReporterDefinition.setRateUnit(DEFAULT_RATE_UNIT);
		consoleReporterDefinition.setPeriodDuration(DEFAULT_PERIOD_DURATION);
		consoleReporterDefinition.setPeriodDurationUnit(DEFAULT_PERIOD_DURATION_UNIT);
		return consoleReporterDefinition;
	}

	// fields
	private String		name;
	private TimeUnit	durationUnit;
	private TimeUnit	rateUnit;
	private Long		periodDuration;
	private TimeUnit	periodDurationUnit;

	@Override
	public ConsoleReporterDefinition applyAsOverride(final ConsoleReporterDefinition override) {
		ConsoleReporterDefinition consoleReporterDefinition = new ConsoleReporterDefinition();
		// get current values
		consoleReporterDefinition.setName(this.name);
		consoleReporterDefinition.setDurationUnit(this.durationUnit);
		consoleReporterDefinition.setRateUnit(this.rateUnit);
		consoleReporterDefinition.setPeriodDuration(this.periodDuration);
		consoleReporterDefinition.setPeriodDurationUnit(this.periodDurationUnit);
		// apply new values
		consoleReporterDefinition.setNameIfNotNull(override.getName());
		consoleReporterDefinition.setDurationUnitIfNotNull(override.getDurationUnit());
		consoleReporterDefinition.setRateUnitIfNotNull(override.getDurationUnit());
		consoleReporterDefinition.setPeriodDurationIfNotNull(override.getPeriodDuration());
		consoleReporterDefinition.setPeriodDurationUnitIfNotNull(override.getPeriodDurationUnit());
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
		return ToStringBuilder.reflectionToString(this);
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
