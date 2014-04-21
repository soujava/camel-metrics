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


/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.2
 * @since 2014-04-17
 */
public class CounterDefinition extends ExpressionMetricDefinition {

	// constants
	private static final String	DEFAULT_NAME_BASE		= "counter";
	private static long			DEFAULT_NAME_BASE_INDEX	= 0;
	private static final String	DEFAULT_VALUE			= "1";

	/**
	 * @return
	 */
	public static synchronized String getNextDefaultName() {
		DEFAULT_NAME_BASE_INDEX++;
		return DEFAULT_NAME_BASE + DEFAULT_NAME_BASE_INDEX;
	}

	// fields
	private String	value	= DEFAULT_VALUE;

	/**
	 * @return the value
	 */
	public String getValue() {
		return this.value;
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
		builder.append("CounterDefinition [value=");
		builder.append(this.value);
		builder.append(", getName()=");
		builder.append(getName());
		builder.append("]");
		return builder.toString();
	}

}
