package io.initium.common.util;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.1
 * @since 2014-02-19
 */
public class OptionHelper {

	/**
	 * @param value
	 * @param type
	 * @return
	 */
	public static <T> T parse(final String value, final Class<T> type) {
		if (Boolean.class.isAssignableFrom(type)) {
			type.cast(parseBoolean(value));
		}
		return null;
	}

	/**
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

}
