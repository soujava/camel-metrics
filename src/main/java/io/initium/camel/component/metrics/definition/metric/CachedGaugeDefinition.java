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
package io.initium.camel.component.metrics.definition.metric;

import java.util.concurrent.TimeUnit;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.2
 * @since 2014-04-17
 */
public class CachedGaugeDefinition extends ExpressionMetricDefinition {

	// constants
	private static final String		DEFAULT_NAME_BASE			= "cachedGauge";
	private static long				DEFAULT_NAME_BASE_INDEX		= 0;
	private static final String		DEFAULT_VALUE				= "1";
	private static final TimeUnit	DEFAULT_CACHE_DURATION_UNIT	= TimeUnit.SECONDS;
	private static final int		DEFAULT_CACHE_DURATION		= 10;

	/**
	 * @return
	 */
	public static synchronized String getNextDefaultName() {
		DEFAULT_NAME_BASE_INDEX++;
		return DEFAULT_NAME_BASE + DEFAULT_NAME_BASE_INDEX;
	}

	// fields
	private String		value			= DEFAULT_VALUE;
	private long		duration		= DEFAULT_CACHE_DURATION;
	private TimeUnit	durationUnit	= DEFAULT_CACHE_DURATION_UNIT;

	/**
	 * @return the duration
	 */
	public long getDuration() {
		return this.duration;
	}

	/**
	 * @return the durationUnit
	 */
	public TimeUnit getDurationUnit() {
		return this.durationUnit;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return this.value;
	}

	/**
	 * @param duration
	 *            the duration to set
	 */
	public void setDuration(final long duration) {
		this.duration = duration;
	}

	/**
	 * @param durationUnit
	 *            the durationUnit to set
	 */
	public void setDurationUnit(final TimeUnit durationUnit) {
		this.durationUnit = durationUnit;
	}

	/**
	 * @param value
	 *            the value to set
	 */
	public void setValue(final String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CachedGaugeDefinition [value=");
		builder.append(this.value);
		builder.append(", duration=");
		builder.append(this.duration);
		builder.append(", durationUnit=");
		builder.append(this.durationUnit);
		builder.append(", getName()=");
		builder.append(getName());
		builder.append("]");
		return builder.toString();
	}

}
