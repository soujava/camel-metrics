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

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.0
 * @since 2014-02-19
 */
public class TimeUnitUtils {

	// logging
	private static final String	SELF	= Thread.currentThread().getStackTrace()[1].getClassName();
	private static final Logger	LOGGER	= LoggerFactory.getLogger(SELF);

	/**
	 * @param timeUnitName
	 * @return
	 */
	public static TimeUnit getTimeUnit(final String timeUnitName) {
		TimeUnit result1 = null;
		TimeUnit result2 = null;
		String timeUnitNameUC = timeUnitName.toUpperCase();
		try {
			result1 = TimeUnit.valueOf(timeUnitNameUC);
		} catch (IllegalArgumentException e) {
			LOGGER.trace("could not find TimeUnit value for: {}", timeUnitName, e);
		}
		if (result1 == null) {
			try {
				result2 = TimeUnit.valueOf(timeUnitNameUC + "S");
			} catch (IllegalArgumentException e) {
				LOGGER.trace("could not find TimeUnit value for: {}", timeUnitName, e);
			}
		}
		return result1 != null ? result1 : result2;
	}

}
