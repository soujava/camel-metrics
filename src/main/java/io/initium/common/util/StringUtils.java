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
