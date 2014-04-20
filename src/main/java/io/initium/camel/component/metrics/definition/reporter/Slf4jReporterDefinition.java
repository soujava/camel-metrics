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
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Slf4jReporter;

import io.initium.camel.component.metrics.MetricGroup;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.1
 * @since 2014-02-19
 */
public class Slf4jReporterDefinition extends AbstractReporterDefinition<Slf4jReporterDefinition> {

	// fields
	private static final String		DEFAULT_NAME						= Slf4jReporterDefinition.class.getSimpleName();
	private static final TimeUnit	DEFAULT_DURATION_UNIT				= TimeUnit.MILLISECONDS;
	private static final TimeUnit	DEFAULT_RATE_UNIT					= TimeUnit.SECONDS;
	private static final long		DEFAULT_PERIOD_DURATION				= 1;
	private static final TimeUnit	DEFAULT_PERIOD_DURATION_UNIT		= TimeUnit.MINUTES;
	private static final String		DEFAULT_LOGGER_NAME					= "metrics";
	private static final String		DEFAULT_RUNTIME_LOGGER_NAME			= null;
	private static final String		DEFAULT_RUNTIME_SIMPLE_LOGGER_NAME	= null;
	private static final String		DEFAULT_MARKER_NAME					= "metrics";
	private static final String		DEFAULT_RUNTIME_MARKER_NAME			= null;
	private static final String		DEFAULT_RUNTIME_SIMPLE_MARKER_NAME	= null;

	/**
	 * @return
	 */
	public static Slf4jReporterDefinition getDefaultReporter() {
		Slf4jReporterDefinition defaultDefinition = new Slf4jReporterDefinition();
		defaultDefinition.setName(DEFAULT_NAME);
		defaultDefinition.setDurationUnit(DEFAULT_DURATION_UNIT);
		defaultDefinition.setRateUnit(DEFAULT_RATE_UNIT);
		defaultDefinition.setPeriodDuration(DEFAULT_PERIOD_DURATION);
		defaultDefinition.setPeriodDurationUnit(DEFAULT_PERIOD_DURATION_UNIT);
		defaultDefinition.setLoggerName(DEFAULT_LOGGER_NAME);
		defaultDefinition.setRuntimeLoggerName(DEFAULT_RUNTIME_LOGGER_NAME);
		defaultDefinition.setRuntimeSimpleLoggerName(DEFAULT_RUNTIME_SIMPLE_LOGGER_NAME);
		defaultDefinition.setMarkerName(DEFAULT_MARKER_NAME);
		defaultDefinition.setRuntimeMarkerName(DEFAULT_RUNTIME_MARKER_NAME);
		defaultDefinition.setRuntimeSimpleMarkerName(DEFAULT_RUNTIME_SIMPLE_MARKER_NAME);
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
	private String		loggerName;
	private String		runtimeLoggerName;
	private String		runtimeSimpleLoggerName;
	private String		markerName;
	private String		runtimeMarkerName;
	private String		runtimeSimpleMarkerName;

	@Override
	public Slf4jReporterDefinition applyAsOverride(final Slf4jReporterDefinition override) {
		Slf4jReporterDefinition combinedDefinition = new Slf4jReporterDefinition();
		// get current values
		combinedDefinition.setName(getName());
		combinedDefinition.setDurationUnit(getDurationUnit());
		combinedDefinition.setRateUnit(getRateUnit());
		combinedDefinition.setPeriodDuration(getPeriodDuration());
		combinedDefinition.setPeriodDurationUnit(getPeriodDurationUnit());
		combinedDefinition.setLoggerName(getLoggerName());
		combinedDefinition.setRuntimeLoggerName(getRuntimeLoggerName());
		combinedDefinition.setRuntimeSimpleLoggerName(getRuntimeSimpleLoggerName());
		combinedDefinition.setMarkerName(getMarkerName());
		combinedDefinition.setRuntimeMarkerName(getRuntimeMarkerName());
		combinedDefinition.setRuntimeSimpleMarkerName(getRuntimeSimpleMarkerName());
		combinedDefinition.setFilter(getFilter());
		combinedDefinition.setRuntimeFilter(getRuntimeFilter());
		combinedDefinition.setRuntimeSimpleFilter(getRuntimeSimpleFilter());
		// apply new values
		combinedDefinition.setNameIfNotNull(override.getName());
		combinedDefinition.setDurationUnitIfNotNull(override.getDurationUnit());
		combinedDefinition.setRateUnitIfNotNull(override.getRateUnit());
		combinedDefinition.setPeriodDurationIfNotNull(override.getPeriodDuration());
		combinedDefinition.setPeriodDurationUnitIfNotNull(override.getPeriodDurationUnit());
		combinedDefinition.setLoggerNameIfNotNull(override.getLoggerName());
		combinedDefinition.setRuntimeLoggerNameIfNotNull(override.getRuntimeLoggerName());
		combinedDefinition.setRuntimeSimpleLoggerNameIfNotNull(override.getRuntimeSimpleLoggerName());
		combinedDefinition.setMarkerNameIfNotNull(override.getMarkerName());
		combinedDefinition.setRuntimeMarkerNameIfNotNull(override.getRuntimeMarkerName());
		combinedDefinition.setRuntimeSimpleMarkerNameIfNotNull(override.getRuntimeSimpleMarkerName());
		combinedDefinition.setFilterIfNotNull(override.getFilter());
		combinedDefinition.setRuntimeFilterIfNotNull(override.getRuntimeFilter());
		combinedDefinition.setRuntimeSimpleFilterIfNotNull(override.getRuntimeSimpleFilter());
		return combinedDefinition;
	}

	/**
	 * @param metricRegistry
	 * @return
	 */
	public Slf4jReporter buildReporter(final MetricRegistry metricRegistry, final Exchange creatingExchange, final MetricGroup metricGroup) {
		Slf4jReporterDefinition definitionWithDefaults = getReporterDefinitionWithDefaults();

		final String filterValue = evaluateValue(definitionWithDefaults.getFilter(), definitionWithDefaults.getRuntimeFilter(), definitionWithDefaults.getRuntimeSimpleFilter(), creatingExchange);
		final String loggerNameValue = evaluateValue(definitionWithDefaults.getLoggerName(), definitionWithDefaults.getRuntimeLoggerName(), definitionWithDefaults.getRuntimeSimpleLoggerName(), creatingExchange);
		final String markerNameValue = evaluateValue(definitionWithDefaults.getMarkerName(), definitionWithDefaults.getRuntimeMarkerName(), definitionWithDefaults.getRuntimeSimpleMarkerName(), creatingExchange);

		// @formatter:off
		Slf4jReporter slf4jReporter = Slf4jReporter
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
				.outputTo(LoggerFactory.getLogger(loggerNameValue))
				.markWith(MarkerFactory.getMarker(markerNameValue))
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

	/**
	 * @return the runtimeLoggerName
	 */
	public String getRuntimeLoggerName() {
		return this.runtimeLoggerName;
	}

	/**
	 * @return the runtimeMarkerName
	 */
	public String getRuntimeMarkerName() {
		return this.runtimeMarkerName;
	}

	/**
	 * @return the runtimeSimpleLoggerName
	 */
	public String getRuntimeSimpleLoggerName() {
		return this.runtimeSimpleLoggerName;
	}

	/**
	 * @return the runtimeSimpleMarkerName
	 */
	public String getRuntimeSimpleMarkerName() {
		return this.runtimeSimpleMarkerName;
	}

	@Override
	public void setDurationUnit(final TimeUnit durationUnit) {
		this.durationUnit = durationUnit;
	}

	/**
	 * @param filter
	 *            the filter to set
	 */
	@Override
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

	/**
	 * @param runtimeLoggerName
	 *            the runtimeLoggerName to set
	 */
	public void setRuntimeLoggerName(final String runtimeLoggerName) {
		this.runtimeLoggerName = runtimeLoggerName;
	}

	/**
	 * @param runtimeMarkerName
	 *            the runtimeMarkerName to set
	 */
	public void setRuntimeMarkerName(final String runtimeMarkerName) {
		this.runtimeMarkerName = runtimeMarkerName;
	}

	/**
	 * @param runtimeSimpleLoggerName
	 *            the runtimeSimpleLoggerName to set
	 */
	public void setRuntimeSimpleLoggerName(final String runtimeSimpleLoggerName) {
		this.runtimeSimpleLoggerName = runtimeSimpleLoggerName;
	}

	/**
	 * @param runtimeSimpleMarkerName
	 *            the runtimeSimpleMarkerName to set
	 */
	public void setRuntimeSimpleMarkerName(final String runtimeSimpleMarkerName) {
		this.runtimeSimpleMarkerName = runtimeSimpleMarkerName;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Slf4jReporterDefinition [name=");
		builder.append(this.name);
		builder.append(", durationUnit=");
		builder.append(this.durationUnit);
		builder.append(", rateUnit=");
		builder.append(this.rateUnit);
		builder.append(", periodDuration=");
		builder.append(this.periodDuration);
		builder.append(", periodDurationUnit=");
		builder.append(this.periodDurationUnit);
		builder.append(", loggerName=");
		builder.append(this.loggerName);
		builder.append(", runtimeLoggerName=");
		builder.append(this.runtimeLoggerName);
		builder.append(", runtimeSimpleLoggerName=");
		builder.append(this.runtimeSimpleLoggerName);
		builder.append(", markerName=");
		builder.append(this.markerName);
		builder.append(", runtimeMarkerName=");
		builder.append(this.runtimeMarkerName);
		builder.append(", runtimeSimpleMarkerName=");
		builder.append(this.runtimeSimpleMarkerName);
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

	/**
	 * @param runtimeLoggerName
	 */
	private void setRuntimeLoggerNameIfNotNull(final String runtimeLoggerName) {
		if (runtimeLoggerName != null) {
			setRuntimeLoggerName(runtimeLoggerName);
		}
	}

	/**
	 * @param runtimeMarkerName
	 */
	private void setRuntimeMarkerNameIfNotNull(final String runtimeMarkerName) {
		if (runtimeMarkerName != null) {
			setRuntimeMarkerName(runtimeMarkerName);
		}
	}

	/**
	 * @param runtimeSimpleLoggerName
	 */
	private void setRuntimeSimpleLoggerNameIfNotNull(final String runtimeSimpleLoggerName) {
		if (runtimeSimpleLoggerName != null) {
			setRuntimeSimpleLoggerName(runtimeSimpleLoggerName);
		}
	}

	/**
	 * @param runtimeSimpleMarkerName
	 */
	private void setRuntimeSimpleMarkerNameIfNotNull(final String runtimeSimpleMarkerName) {
		if (runtimeSimpleMarkerName != null) {
			setRuntimeSimpleMarkerName(runtimeSimpleMarkerName);
		}
	}

}
