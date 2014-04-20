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

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;

import com.codahale.metrics.CsvReporter;
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
public class CsvReporterDefinition extends AbstractReporterDefinition<CsvReporterDefinition> {

	// fields
	private static final String		DEFAULT_NAME						= CsvReporterDefinition.class.getSimpleName();
	private static final TimeUnit	DEFAULT_DURATION_UNIT				= TimeUnit.MILLISECONDS;
	private static final TimeUnit	DEFAULT_RATE_UNIT					= TimeUnit.SECONDS;
	private static final long		DEFAULT_PERIOD_DURATION				= 1;
	private static final TimeUnit	DEFAULT_PERIOD_DURATION_UNIT		= TimeUnit.MINUTES;
	private static final String		DEFAULT_DIRECTORY					= ".";
	private static final String		DEFAULT_RUNTIME_DIRECTORY			= null;
	private static final String		DEFAULT_RUNTIME_SIMPLE_DIRECTORY	= null;

	/**
	 * @return
	 */
	public static CsvReporterDefinition getDefaultReporter() {
		CsvReporterDefinition defaultDefinition = new CsvReporterDefinition();
		defaultDefinition.setName(DEFAULT_NAME);
		defaultDefinition.setDurationUnit(DEFAULT_DURATION_UNIT);
		defaultDefinition.setRateUnit(DEFAULT_RATE_UNIT);
		defaultDefinition.setPeriodDuration(DEFAULT_PERIOD_DURATION);
		defaultDefinition.setPeriodDurationUnit(DEFAULT_PERIOD_DURATION_UNIT);
		defaultDefinition.setDirectory(DEFAULT_DIRECTORY);
		defaultDefinition.setRuntimeDirectory(DEFAULT_RUNTIME_DIRECTORY);
		defaultDefinition.setRuntimeSimpleDirectory(DEFAULT_RUNTIME_SIMPLE_DIRECTORY);
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
	private String		directory;
	private String		runtimeDirectory;
	private String		runtimeSimpleDirectory;

	@Override
	public CsvReporterDefinition applyAsOverride(final CsvReporterDefinition override) {
		CsvReporterDefinition combinedDefinition = new CsvReporterDefinition();
		// get current values
		combinedDefinition.setName(getName());
		combinedDefinition.setDurationUnit(getDurationUnit());
		combinedDefinition.setRateUnit(getRateUnit());
		combinedDefinition.setPeriodDuration(getPeriodDuration());
		combinedDefinition.setPeriodDurationUnit(getPeriodDurationUnit());
		combinedDefinition.setDirectory(getDirectory());
		combinedDefinition.setRuntimeDirectory(getRuntimeDirectory());
		combinedDefinition.setRuntimeSimpleDirectory(getRuntimeSimpleDirectory());
		combinedDefinition.setFilter(getFilter());
		combinedDefinition.setRuntimeFilter(getRuntimeFilter());
		combinedDefinition.setRuntimeSimpleFilter(getRuntimeSimpleFilter());
		// apply new values
		combinedDefinition.setNameIfNotNull(override.getName());
		combinedDefinition.setDurationUnitIfNotNull(override.getDurationUnit());
		combinedDefinition.setRateUnitIfNotNull(override.getRateUnit());
		combinedDefinition.setPeriodDurationIfNotNull(override.getPeriodDuration());
		combinedDefinition.setPeriodDurationUnitIfNotNull(override.getPeriodDurationUnit());
		combinedDefinition.setDirectoryIfNotNull(override.getDirectory());
		combinedDefinition.setRuntimeDirectoryIfNotNull(override.getRuntimeDirectory());
		combinedDefinition.setRuntimeSimpleDirectoryIfNotNull(override.getRuntimeSimpleDirectory());
		combinedDefinition.setFilterIfNotNull(override.getFilter());
		combinedDefinition.setRuntimeFilterIfNotNull(override.getRuntimeFilter());
		combinedDefinition.setRuntimeSimpleFilterIfNotNull(override.getRuntimeSimpleFilter());
		return combinedDefinition;
	}

	/**
	 * @param metricRegistry
	 * @return
	 */
	public CsvReporter buildReporter(final MetricRegistry metricRegistry, final Exchange creatingExchange, final MetricGroup metricGroup) {
		CsvReporterDefinition definitionWithDefaults = getReporterDefinitionWithDefaults();

		final String filterValue = evaluateValue(definitionWithDefaults.getFilter(), definitionWithDefaults.getRuntimeFilter(), definitionWithDefaults.getRuntimeSimpleFilter(), creatingExchange);
		final String directoryValue = evaluateValue(definitionWithDefaults.getDirectory(), definitionWithDefaults.getRuntimeDirectory(), definitionWithDefaults.getRuntimeSimpleDirectory(), creatingExchange);

		// @formatter:off
		CsvReporter csvReporter = CsvReporter
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
				.build(new File(directoryValue));
		// @formatter:on
		return csvReporter;
	}

	/**
	 * @return the directory
	 */
	public String getDirectory() {
		return this.directory;
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
	public CsvReporterDefinition getReporterDefinitionWithDefaults() {
		return getDefaultReporter().applyAsOverride(this);
	}

	/**
	 * @return the runtimeDirectory
	 */
	public String getRuntimeDirectory() {
		return this.runtimeDirectory;
	}

	/**
	 * @return the runtimeSimpleDirectory
	 */
	public String getRuntimeSimpleDirectory() {
		return this.runtimeSimpleDirectory;
	}

	/**
	 * @param directory
	 *            the directory to set
	 */
	public void setDirectory(final String directory) {
		this.directory = directory;
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

	/**
	 * @param runtimeDirectory
	 *            the runtimeDirectory to set
	 */
	public void setRuntimeDirectory(final String runtimeDirectory) {
		this.runtimeDirectory = runtimeDirectory;
	}

	/**
	 * @param runtimeDirectory
	 *            the runtimeDirectory to set
	 */
	public void setRuntimeDirectoryIfNotNull(final String runtimeDirectory) {
		if (runtimeDirectory != null) {
			setRuntimeDirectory(runtimeDirectory);
		}
	}

	/**
	 * @param runtimeSimpleDirectory
	 *            the runtimeSimpleDirectory to set
	 */
	public void setRuntimeSimpleDirectory(final String runtimeSimpleDirectory) {
		this.runtimeSimpleDirectory = runtimeSimpleDirectory;
	}

	/**
	 * @param runtimeSimpleDirectory
	 *            the runtimeSimpleDirectory to set
	 */
	public void setRuntimeSimpleDirectoryIfNotNull(final String runtimeSimpleDirectory) {
		if (runtimeSimpleDirectory != null) {
			setRuntimeSimpleDirectory(runtimeSimpleDirectory);
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CsvReporterDefinition [name=");
		builder.append(this.name);
		builder.append(", durationUnit=");
		builder.append(this.durationUnit);
		builder.append(", rateUnit=");
		builder.append(this.rateUnit);
		builder.append(", periodDuration=");
		builder.append(this.periodDuration);
		builder.append(", periodDurationUnit=");
		builder.append(this.periodDurationUnit);
		builder.append(", directory=");
		builder.append(this.directory);
		builder.append(", runtimeDirectory=");
		builder.append(this.runtimeDirectory);
		builder.append(", runtimeSimpleDirectory=");
		builder.append(this.runtimeSimpleDirectory);
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
	 * @param directory
	 */
	private void setDirectoryIfNotNull(final String directory) {
		if (directory != null) {
			setDirectory(directory);
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
	 * @param periodDuration
	 */
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
