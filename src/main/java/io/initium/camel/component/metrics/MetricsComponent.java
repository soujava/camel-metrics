package io.initium.camel.component.metrics;

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.UriEndpointComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.0
 * @since 2014-02-19
 */
public class MetricsComponent extends UriEndpointComponent {

	// logging
	private static final String	SELF						= Thread.currentThread().getStackTrace()[1].getClassName();
	private static final Logger	LOGGER						= LoggerFactory.getLogger(SELF);

	// constants
	public static final Marker	MARKER						= MarkerFactory.getMarker("METRICS");
	public static final String	DEFALUT_JMX_REPORTER_DOMAIN	= "io.initium.metrics";
	public static final String	TIMER_CONTEXT_MAP_NAME		= DEFALUT_JMX_REPORTER_DOMAIN + ".TimingMap";

	/**
	 * 
	 */
	public MetricsComponent() {
		super(MetricsEndpoint.class);
		LOGGER.debug(MARKER, "MetricsComponent()");
	}

	@Override
	protected Endpoint createEndpoint(final String uri, final String remaining, final Map<String, Object> parameters) throws Exception {
		LOGGER.debug(MARKER, "createEndpoint({},{},{})", uri, remaining, parameters);
		MetricsEndpoint endpoint = new MetricsEndpoint(uri, this, remaining, parameters);
		setProperties(endpoint, parameters);
		return endpoint;
	}

	@Override
	protected void doResume() throws Exception {
		super.doResume();
		LOGGER.debug(MARKER, "doResume()");
	}

	@Override
	protected void doShutdown() throws Exception {
		super.doShutdown();
		LOGGER.debug(MARKER, "doShutdown()");
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();
		LOGGER.debug(MARKER, "doStart()");
	}

	@Override
	protected void doStop() throws Exception {
		super.doStop();
		LOGGER.debug(MARKER, "doStop()");
	}

	@Override
	protected void doSuspend() throws Exception {
		super.doSuspend();
		LOGGER.debug(MARKER, "doSuspend()");
	}

}
