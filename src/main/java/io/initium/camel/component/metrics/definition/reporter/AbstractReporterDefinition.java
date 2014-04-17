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
import org.apache.camel.Expression;
import org.apache.camel.spi.Language;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.2
 * @since 2014-04-17
 */
public abstract class AbstractReporterDefinition<T extends ReporterDefinition<T>> implements ReporterDefinition<T> {

	/**
	 * @param value
	 * @param creatingExchange
	 * @param type
	 * @return
	 */
	public static <T> T evaluateExpression(final String value, final Exchange creatingExchange, final Class<T> type) {
		if (value == null) {
			return null;
		}
		Language language;
		if (value.contains("$")) {
			language = creatingExchange.getContext().resolveLanguage("file");
		} else {
			language = creatingExchange.getContext().resolveLanguage("constant");
		}
		Expression filterExpression = language.createExpression(value);
		return filterExpression.evaluate(creatingExchange, type);
	}
}
