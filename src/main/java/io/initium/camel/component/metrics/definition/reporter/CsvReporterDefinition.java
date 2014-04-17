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
	private static final String		DEFAULT_NAME					= CsvReporterDefinition.class.getSimpleName();
	private static final TimeUnit	DEFAULT_DURATION_UNIT			= TimeUnit.MILLISECONDS;
	private static final TimeUnit	DEFAULT_RATE_UNIT				= TimeUnit.SECONDS;
	private static final long		DEFAULT_PERIOD_DURATION			= 1;
	private static final TimeUnit	DEFAULT_PERIOD_DURATION_UNIT	= TimeUnit.MINUTES;
	private static final String		DEFAULT_FILTER					= null;
	private static final String		DEFAULT_DYNAMIC_FILTER			= null;
	private static final String		DEFAULT_DIRECTORY				= ".";
	private static final String		DEFAULT_DYNAMIC_DIRECTORY		= null;

	/**
	 * @return
	 */
	public static CsvReporterDefinition getDefaultReporter() {
		CsvReporterDefinition csvReporterDefinition = new CsvReporterDefinition();
		csvReporterDefinition.setName(DEFAULT_NAME);
		csvReporterDefinition.setDurationUnit(DEFAULT_DURATION_UNIT);
		csvReporterDefinition.setRateUnit(DEFAULT_RATE_UNIT);
		csvReporterDefinition.setPeriodDuration(DEFAULT_PERIOD_DURATION);
		csvReporterDefinition.setPeriodDurationUnit(DEFAULT_PERIOD_DURATION_UNIT);
		csvReporterDefinition.setFilter(DEFAULT_FILTER);
		csvReporterDefinition.setDynamicFilter(DEFAULT_DYNAMIC_FILTER);
		csvReporterDefinition.setDirectory(DEFAULT_DIRECTORY);
		csvReporterDefinition.setDynamicDirectory(DEFAULT_DYNAMIC_DIRECTORY);
		return csvReporterDefinition;
	}

	// fields
	private String		name	= DEFAULT_NAME;
	private TimeUnit	durationUnit;
	private TimeUnit	rateUnit;
	private Long		periodDuration;
	private TimeUnit	periodDurationUnit;
	private String		filter;
	private String		dynamicFilter;
	private String		directory;
	private String		dynamicDirectory;

	@Override
	public CsvReporterDefinition applyAsOverride(final CsvReporterDefinition override) {
		CsvReporterDefinition csvReporterDefinition = new CsvReporterDefinition();
		// get current values
		csvReporterDefinition.setName(this.name);
		csvReporterDefinition.setDurationUnit(this.durationUnit);
		csvReporterDefinition.setRateUnit(this.rateUnit);
		csvReporterDefinition.setPeriodDuration(this.periodDuration);
		csvReporterDefinition.setPeriodDurationUnit(this.periodDurationUnit);
		csvReporterDefinition.setFilter(this.filter);
		csvReporterDefinition.setDynamicFilter(this.dynamicFilter);
		csvReporterDefinition.setDirectory(this.directory);
		csvReporterDefinition.setDynamicDirectory(this.dynamicDirectory);
		// apply new values
		csvReporterDefinition.setNameIfNotNull(override.getName());
		csvReporterDefinition.setDurationUnitIfNotNull(override.getDurationUnit());
		csvReporterDefinition.setRateUnitIfNotNull(override.getRateUnit());
		csvReporterDefinition.setPeriodDurationIfNotNull(override.getPeriodDuration());
		csvReporterDefinition.setPeriodDurationUnitIfNotNull(override.getPeriodDurationUnit());
		csvReporterDefinition.setFilterIfNotNull(override.getFilter());
		csvReporterDefinition.setDynamicFilterIfNotNull(override.getDynamicFilter());
		csvReporterDefinition.setDirectoryIfNotNull(override.getDirectory());
		csvReporterDefinition.setDynamicDirectoryIfNotNull(override.getDynamicDirectory());
		return csvReporterDefinition;
	}

	/**
	 * @param metricRegistry
	 * @return
	 */
	public CsvReporter buildReporter(final MetricRegistry metricRegistry, final Exchange creatingExchange, final MetricGroup metricGroup) {
		CsvReporterDefinition csvReporterDefinition = getReporterDefinitionWithDefaults();

		final String evaluatedFilter = creatingExchange == null ? this.filter : evaluateExpression(csvReporterDefinition.getDynamicFilter(), creatingExchange, String.class);
		final String evaluatedDirectory = creatingExchange == null ? this.directory : evaluateExpression(csvReporterDefinition.getDynamicDirectory(), creatingExchange, String.class);

		// @formatter:off
		CsvReporter csvReporter = CsvReporter
				.forRegistry(metricRegistry)
				.convertDurationsTo(csvReporterDefinition.getDurationUnit())
				.convertRatesTo(csvReporterDefinition.getRateUnit())
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
				.build(new File(evaluatedDirectory));
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

	/**
	 * @return the dynamicDirectory
	 */
	public String getDynamicDirectory() {
		return this.dynamicDirectory;
	}

	/**
	 * @return the dynamicFilter
	 */
	public String getDynamicFilter() {
		return this.dynamicFilter;
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
	public CsvReporterDefinition getReporterDefinitionWithDefaults() {
		return getDefaultReporter().applyAsOverride(this);
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

	/**
	 * @param dynamicDirectory
	 *            the dynamicDirectory to set
	 */
	public void setDynamicDirectory(final String dynamicDirectory) {
		this.dynamicDirectory = dynamicDirectory;
	}

	/**
	 * @param dynamicDirectory
	 *            the dynamicDirectory to set
	 */
	public void setDynamicDirectoryIfNotNull(final String dynamicDirectory) {
		if (dynamicDirectory != null) {
			setDynamicDirectory(dynamicDirectory);
		}
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
		return "CsvReporterDefinition [name=" + this.name + ", durationUnit=" + this.durationUnit + ", rateUnit=" + this.rateUnit + ", periodDuration=" + this.periodDuration + ", periodDurationUnit=" + this.periodDurationUnit + ", filter=" + this.filter
				+ ", dynamicFilter=" + this.dynamicFilter + ", directory=" + this.directory + ", dynamicDirectory=" + this.dynamicDirectory + "]";
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
