package io.initium.camel.component.metrics;

import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @version 1.0
 * @since 2014-02-19
 */
public class MetricsConsumer extends DefaultConsumer {

	// logging
	private static final String	SELF	= Thread.currentThread().getStackTrace()[1].getClassName();
	private static final Logger	LOGGER	= LoggerFactory.getLogger(SELF);

	public MetricsConsumer(final Endpoint endpoint, final Processor processor) {
		super(endpoint, processor);
		LOGGER.info("MetricsConsumer(,{},{})", endpoint, processor);
	}

}
