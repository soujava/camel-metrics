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
package io.initium.common.util;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.spi.Language;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.2
 * @since 2014-04-21
 */
public class ExpressionUtils {

	// logging
	private static final String	SELF	= Thread.currentThread().getStackTrace()[1].getClassName();

	// private static final Logger LOGGER = LogManager.getLogger(SELF);

	/**
	 * @param value
	 * @param camelContext
	 * @return
	 */
	public static Expression createExpression(final String value, final CamelContext camelContext) {
		if (value == null || camelContext == null) {
			return null;
		}
		Language language;
		if (value.contains("$")) {
			language = camelContext.resolveLanguage("file");
		} else {
			language = camelContext.resolveLanguage("constant");
		}
		return language.createExpression(value);
	}

	/**
	 * @param value
	 * @param exchange
	 * @param type
	 * @return
	 */
	public static <T> T evaluateAsExpression(final String value, final Exchange exchange, final Class<T> type) {
		if (exchange == null || value == null || type == null) {
			return null;
		}
		Expression expression = createExpression(value, exchange.getContext());
		if (expression == null) {
			return null;
		}
		return expression.evaluate(exchange, type);
	}

	/**
	 * This class is not intended to ever be instantiated.
	 */
	private ExpressionUtils() {
		throw new AbstractMethodError("this class, [" + SELF + "], is not intended to ever be instantiated");
	}

}
