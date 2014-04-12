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

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.1
 * @since 2014-04-08
 */
public class OptionHelper {

	// logging
	private static final String	SELF	= Thread.currentThread().getStackTrace()[1].getClassName();
	private static final Logger	LOGGER	= LoggerFactory.getLogger(SELF);

	// constants
	private static Gson			GSON	= new Gson();

	/**
	 * @param value
	 * @param type
	 * @return
	 */
	public static <T> T parse(final String value, final Class<T> type) {
		if (Boolean.class.isAssignableFrom(type)) {
			return type.cast(parseBoolean(value));
		} else if (TimeUnit.class.isAssignableFrom(type)) {
			return type.cast(parseTimeUnit(value));
		} else if (List.class.isAssignableFrom(type)) {
			return type.cast(parseList(value));
		}
		return null;
	}

	/**
	 * Parses the value for a Boolean.
	 * 
	 * @param value
	 * @return
	 */
	private static Boolean parseBoolean(final String value) {
		if ("1".equals(value)) {
			return true;
		} else if ("yes".equalsIgnoreCase(value)) {
			return true;
		}
		return Boolean.parseBoolean(value);
	}

	/**
	 * Parses the value for a List. Assumes the value is kind like JSON.
	 * 
	 * @param value
	 * @return
	 */
	private static List<String> parseList(final String value) {
		Type listType = new TypeToken<List<?>>() {}.getType();
		List<String> result = null;
		String lclValue = value;
		try {
			result = GSON.fromJson(lclValue, listType);
		} catch (Exception e) {
			LOGGER.debug("could not parse raw value as List: {}", value);
		}
		if (result == null) {
			try {
				lclValue = "[" + value + "]";
				result = GSON.fromJson(lclValue, listType);
			} catch (Exception e) {
				LOGGER.debug("could not parse encapsulated value as List: {}", lclValue);
			}
		}
		return result;
	}

	/**
	 * Parses the value for a TimeUnit.
	 * 
	 * @param value
	 * @return
	 */
	private static TimeUnit parseTimeUnit(final String value) {
		TimeUnit result1 = null;
		TimeUnit result2 = null;
		String valueUC = value.toUpperCase();
		try {
			result1 = TimeUnit.valueOf(valueUC);
		} catch (IllegalArgumentException e) {
			LOGGER.trace("could not find TimeUnit value for: {}", value, e);
		}
		if (result1 == null) {
			try {
				result2 = TimeUnit.valueOf(valueUC + "S");
			} catch (IllegalArgumentException e) {
				LOGGER.trace("could not find TimeUnit value for: {}", value, e);
			}
		}
		return result1 != null ? result1 : result2;
	}

}
