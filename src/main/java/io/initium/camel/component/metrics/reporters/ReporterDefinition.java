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

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.1
 * @since 2014-02-19
 */
public interface ReporterDefinition<T extends ReporterDefinition<T>> {

	/**
	 * @param metricReporterDefinition
	 * @return
	 */
	T applyAsOverride(T metricReporterDefinition);

	/**
	 * Gets the name of the ReporterDefinition.
	 * 
	 * @return the name of the ReporterDefinition
	 */
	String getName();

	/**
	 * @param metricRegistry
	 * @return
	 */
	T getReporterDefinitionWithDefaults();

	/**
	 * Sets the TimeUnit used for duration metrics with this reporter.
	 * 
	 * @param timeUnit
	 *            the TimeUnit used for duration metrics with this reporter
	 */
	void setDurationUnit(final TimeUnit unit);

	/**
	 * Sets the name of the ReporterDefinition.
	 * 
	 * @param name
	 *            the name of the ReporterDefinition
	 */
	void setName(String name);

	/**
	 * Sets the TimeUnit used for rate metrics with this reporter.
	 * 
	 * @param timeUnit
	 *            the TimeUnit used for rate metrics with this reporter
	 */
	void setRateUnit(final TimeUnit unit);

}
