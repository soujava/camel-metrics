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

import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.1
 * @since 2014-02-19
 */
public class Slf4jReporterDefinition implements ReporterDefinition<Slf4jReporterDefinition> {

	// fields
	private static final String		DEFAULT_NAME					= Slf4jReporterDefinition.class.getSimpleName();
	private static final TimeUnit	DEFAULT_DURATION_UNIT			= TimeUnit.MILLISECONDS;
	private static final TimeUnit	DEFAULT_RATE_UNIT				= TimeUnit.SECONDS;
	private static final long		DEFAULT_PERIOD_DURATION			= 1;
	private static final TimeUnit	DEFAULT_PERIOD_DURATION_UNIT	= TimeUnit.MINUTES;
	private static final String		DEFAULT_FILTER					= null;
	private static final String		DEFAULT_LOGGER_NAME				= "metrics";
	private static final String		DEFAULT_MARKER_NAME				= "metrics";

	/**
	 * @return
	 */
	public static Slf4jReporterDefinition getDefaultReporter() {
		Slf4jReporterDefinition slf4jReporterDefinition = new Slf4jReporterDefinition();
		slf4jReporterDefinition.setName(DEFAULT_NAME);
		slf4jReporterDefinition.setDurationUnit(DEFAULT_DURATION_UNIT);
		slf4jReporterDefinition.setRateUnit(DEFAULT_RATE_UNIT);
		slf4jReporterDefinition.setPeriodDuration(DEFAULT_PERIOD_DURATION);
		slf4jReporterDefinition.setPeriodDurationUnit(DEFAULT_PERIOD_DURATION_UNIT);
		slf4jReporterDefinition.setFilter(DEFAULT_FILTER);
		slf4jReporterDefinition.setLoggerName(DEFAULT_LOGGER_NAME);
		slf4jReporterDefinition.setMarkerName(DEFAULT_MARKER_NAME);
		return slf4jReporterDefinition;
	}

	// fields
	private String		name	= DEFAULT_NAME;
	private TimeUnit	durationUnit;
	private TimeUnit	rateUnit;
	private Long		periodDuration;
	private TimeUnit	periodDurationUnit;
	private String		filter;
	private String		loggerName;
	private String		markerName;

	@Override
	public Slf4jReporterDefinition applyAsOverride(final Slf4jReporterDefinition override) {
		Slf4jReporterDefinition slf4jReporterDefinition = new Slf4jReporterDefinition();
		// get current values
		slf4jReporterDefinition.setName(this.name);
		slf4jReporterDefinition.setDurationUnit(this.durationUnit);
		slf4jReporterDefinition.setRateUnit(this.rateUnit);
		slf4jReporterDefinition.setPeriodDuration(this.periodDuration);
		slf4jReporterDefinition.setPeriodDurationUnit(this.periodDurationUnit);
		slf4jReporterDefinition.setFilter(this.filter);
		slf4jReporterDefinition.setLoggerName(this.loggerName);
		slf4jReporterDefinition.setMarkerName(this.markerName);
		// apply new values
		slf4jReporterDefinition.setNameIfNotNull(override.getName());
		slf4jReporterDefinition.setDurationUnitIfNotNull(override.getDurationUnit());
		slf4jReporterDefinition.setRateUnitIfNotNull(override.getDurationUnit());
		slf4jReporterDefinition.setPeriodDurationIfNotNull(override.getPeriodDuration());
		slf4jReporterDefinition.setPeriodDurationUnitIfNotNull(override.getPeriodDurationUnit());
		slf4jReporterDefinition.setFilterIfNotNull(override.getFilter());
		slf4jReporterDefinition.setLoggerNameIfNotNull(override.getLoggerName());
		slf4jReporterDefinition.setMarkerNameIfNotNull(override.getMarkerName());
		return slf4jReporterDefinition;
	}

	/**
	 * @param metricRegistry
	 * @return
	 */
	public Slf4jReporter buildReporter(final MetricRegistry metricRegistry) {
		Slf4jReporterDefinition slf4jReporterDefinition = getReporterDefinitionWithDefaults();
		// @formatter:off
		Slf4jReporter slf4jReporter = Slf4jReporter
				.forRegistry(metricRegistry)
				.convertDurationsTo(slf4jReporterDefinition.getDurationUnit())
				.convertRatesTo(slf4jReporterDefinition.getRateUnit())
				.filter(new MetricFilter(){
					@Override
					public boolean matches(final String name, final Metric metric) {
						if(name==null || Slf4jReporterDefinition.this.filter==null){
							return true;
						}
						boolean result = name.matches(Slf4jReporterDefinition.this.filter);
						return result;
					}
				})
				.outputTo(LoggerFactory.getLogger(slf4jReporterDefinition.getLoggerName()))
				.markWith(MarkerFactory.getMarker(slf4jReporterDefinition.getMarkerName()))
				.build();
		// @formatter:on
		return slf4jReporter;
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

	/**
	 * @return the loggerName
	 */
	public String getLoggerName() {
		return this.loggerName;
	}

	/**
	 * @return the markerName
	 */
	public String getMarkerName() {
		return this.markerName;
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
	public Slf4jReporterDefinition getReporterDefinitionWithDefaults() {
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

	/**
	 * @param loggerName
	 *            the loggerName to set
	 */
	public void setLoggerName(final String loggerName) {
		this.loggerName = loggerName;
	}

	/**
	 * @param markerName
	 *            the markerName to set
	 */
	public void setMarkerName(final String markerName) {
		this.markerName = markerName;
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
	 * @param loggerName
	 */
	private void setLoggerNameIfNotNull(final String loggerName) {
		if (loggerName != null) {
			setLoggerName(loggerName);
		}
	}

	/**
	 * @param markerName
	 */
	private void setMarkerNameIfNotNull(final String markerName) {
		if (markerName != null) {
			setMarkerName(markerName);
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
