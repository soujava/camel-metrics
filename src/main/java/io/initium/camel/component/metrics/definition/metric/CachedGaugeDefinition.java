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

import org.apache.camel.Expression;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.2
 * @since 2014-02-19
 */
public class CachedGaugeDefinition extends GaugeDefinition {

	// constants
	private static final TimeUnit	DEFAULT_CACHE_DURATION_UNIT	= TimeUnit.SECONDS;
	private static final int		DEFAULT_CACHE_DURATION		= 10;

	// fields
	private long					cacheDuration				= DEFAULT_CACHE_DURATION;
	private TimeUnit				cacheDurationUnit			= DEFAULT_CACHE_DURATION_UNIT;

	/**
	 * @param name
	 * @param expression
	 * @param cacheDuration
	 * @param cacheDurationUnit
	 */
	public CachedGaugeDefinition(final String name, final Expression expression, final long cacheDuration, final TimeUnit cacheDurationUnit) {
		super(name, expression);
		setCacheDuration(cacheDuration);
		setCacheDurationUnit(cacheDurationUnit);
	}

	/**
	 * @return the cacheDuration
	 */
	public long getCacheDuration() {
		return this.cacheDuration;
	}

	/**
	 * @return the cacheDurationUnit
	 */
	public TimeUnit getCacheDurationUnit() {
		return this.cacheDurationUnit;
	}

	/**
	 * @param cacheDuration
	 *            the cacheDuration to set
	 */
	public void setCacheDuration(final long cacheDuration) {
		this.cacheDuration = cacheDuration;
	}

	/**
	 * @param cacheDurationUnit
	 *            the cacheDurationUnit to set
	 */
	public void setCacheDurationUnit(final TimeUnit cacheDurationUnit) {
		this.cacheDurationUnit = cacheDurationUnit;
	}

}
