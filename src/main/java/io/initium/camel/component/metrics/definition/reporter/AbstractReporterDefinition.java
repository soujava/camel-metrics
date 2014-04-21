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

import org.apache.camel.Exchange;

import io.initium.common.util.ExpressionUtils;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.2
 * @since 2014-04-17
 */
public abstract class AbstractReporterDefinition<T extends ReporterDefinition<T>> implements ReporterDefinition<T> {

	// constants
	public static final String	DEFAULT_FILTER					= null;
	public static final String	DEFAULT_RUNTIME_FILTER			= null;
	public static final String	DEFAULT_RUNTIME_SIMPLE_FILTER	= null;

	/**
	 * @param value
	 * @param runtimeValue
	 * @param runtimeSimpleValue
	 * @param creatingExchange
	 * @return
	 */
	public static String evaluateValue(final String value, final String runtimeValue, final String runtimeSimpleValue, final Exchange creatingExchange) {
		if (creatingExchange != null) {
			final String evaluatedValue;
			if (runtimeSimpleValue == null) {
				evaluatedValue = runtimeValue;
			} else {
				evaluatedValue = ExpressionUtils.evaluateAsExpression(runtimeSimpleValue, creatingExchange, String.class);
			}
			if (evaluatedValue != null) {
				return evaluatedValue;
			}
		}
		return value;
	}

	private String	filter;
	private String	runtimeFilter;
	private String	runtimeSimpleFilter;

	/**
	 * @return the filter
	 */
	public String getFilter() {
		return this.filter;
	}

	/**
	 * @return the runtimeFilter
	 */
	public String getRuntimeFilter() {
		return this.runtimeFilter;
	}

	/**
	 * @return the runtimeSimpleFilter
	 */
	public String getRuntimeSimpleFilter() {
		return this.runtimeSimpleFilter;
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
	 * @param runtimeFilter
	 *            the runtimeFilter to set
	 */
	public void setRuntimeFilter(final String runtimeFilter) {
		this.runtimeFilter = runtimeFilter;
	}

	/**
	 * @param runtimeFilter
	 *            the runtimeFilter to set
	 */
	public void setRuntimeFilterIfNotNull(final String runtimeFilter) {
		if (runtimeFilter != null) {
			setRuntimeFilter(runtimeFilter);
		}
	}

	/**
	 * @param runtimeSimpleFilter
	 *            the runtimeSimpleFilter to set
	 */
	public void setRuntimeSimpleFilter(final String runtimeSimpleFilter) {
		this.runtimeSimpleFilter = runtimeSimpleFilter;
	}

	/**
	 * @param runtimeSimpleFilter
	 *            the runtimeSimpleFilter to set
	 */
	public void setRuntimeSimpleFilterIfNotNull(final String runtimeSimpleFilter) {
		if (runtimeSimpleFilter != null) {
			setRuntimeSimpleFilter(runtimeSimpleFilter);
		}
	}

}
