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

import com.codahale.metrics.MetricRegistry;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.2
 * @since 2014-02-19
 */
public final class MetricUtils {

	// logging
	private static final String	SELF	= Thread.currentThread().getStackTrace()[1].getClassName();

	// private static final Logger LOGGER = LogManager.getLogger(SELF);

	/**
	 * @param baseName
	 * @param infixName
	 * @return
	 */
	public static String calculateFullMetricName(final String baseName, final String infixName) {
		if (infixName == null) {
			return baseName;
		}
		return MetricRegistry.name(baseName, infixName);
	}

	/**
	 * This class is not intended to ever be instantiated.
	 */
	private MetricUtils() {
		throw new AbstractMethodError("this class [" + SELF + "] is not intended to ever be instantiated");
	}

}
