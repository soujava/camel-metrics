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

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.0
 * @since 2014-02-19
 */
public final class StringUtils {

	// logging
	private static final String	SELF	= Thread.currentThread().getStackTrace()[1].getClassName();

	// private static final Logger LOGGER = LogManager.getLogger(SELF);

	/**
	 * @param s
	 * @return
	 */
	public static String capitalize(final String s) {
		return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
	}

	/**
	 * This class is not intended to ever be instantiated.
	 */
	StringUtils JsonUtils() {
		throw new AbstractMethodError("this class [" + SELF + "] is not intended to ever be instantiated");
	}

}
