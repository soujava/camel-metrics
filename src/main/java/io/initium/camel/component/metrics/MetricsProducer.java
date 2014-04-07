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
package io.initium.camel.component.metrics;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

import static io.initium.camel.component.metrics.MetricsComponent.MARKER;
import static io.initium.camel.component.metrics.MetricsComponent.TIMER_CONTEXT_MAP_NAME;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @version 1.0
 * @since 2014-02-19
 */
public class MetricsProducer extends DefaultProducer {

	// logging
	private static final String	SELF	= Thread.currentThread().getStackTrace()[1].getClassName();
	private static final Logger	LOGGER	= LoggerFactory.getLogger(SELF);

	/**
	 * @param exchange
	 * @return
	 */
	private static Map<String, Context> getTimerContextMap(final Exchange exchange) {
		@SuppressWarnings("unchecked")
		Map<String, Context> timerContextMap = exchange.getProperty(TIMER_CONTEXT_MAP_NAME, Map.class);
		if (timerContextMap != null) {
			return timerContextMap;
		}
		timerContextMap = new HashMap<String, Context>();
		exchange.setProperty(TIMER_CONTEXT_MAP_NAME, timerContextMap);
		return timerContextMap;
	}

	// fields
	private final MetricsEndpoint	endpoint;

	/**
	 * @param endpoint
	 */
	public MetricsProducer(final MetricsEndpoint endpoint) {
		super(endpoint);
		LOGGER.debug(MARKER, "MetricsProducer({})", endpoint);
		this.endpoint = endpoint;
	}

	@Override
	public Endpoint getEndpoint() {
		return this.endpoint;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	// TODO process method is too long, refactoring is needed
	// TODO enforce context checking for looking up values in the context map

	@Override
	public void process(final Exchange exchange) throws Exception {
		LOGGER.debug(MARKER, "process({})", exchange);
		Context internalContext = null;
		if (this.endpoint.isInternalTimerEnabled()) {
			Timer internalTimer = this.endpoint.getInternalTimer();
			if (internalTimer != null) {
				internalContext = internalTimer.time();
			}
		}
		try {
			switch (this.endpoint.getTimingAction()) {
			// TODO consider more efficient means for storing Timer contexts
				case START:
					this.endpoint.mark(exchange);
					Map<String, Context> contextMapStart = getTimerContextMap(exchange);
					if (contextMapStart != null) {
						// stop previous context if it exists
						Context context = contextMapStart.get(this.endpoint.getName());
						if (context != null) {
							context.stop();
						}
						// start new context
						context = this.endpoint.getTimer().time();
						contextMapStart.put(this.endpoint.getName(), context);
					} else {
						LOGGER.warn(MARKER, "contextMap is null, timing will not be recorded correctly");
					}
				case NOOP:
					// set lastExchange in endpoint for optional gauge
					this.endpoint.mark(exchange);
					// optional counter
					Expression counterDeltaExpression = this.endpoint.getCounterDelta();
					if (counterDeltaExpression != null) {
						Long delta = counterDeltaExpression.evaluate(exchange, Long.class);
						if (delta != null) {
							Counter counter = this.endpoint.getCounter();
							if (counter != null) {
								counter.inc(delta);
							}
						} else {
							LOGGER.warn(MARKER, "counterDelta does not evaluate to a Long");
						}
					}
					// optional histogram
					Expression histogramValueExpression = this.endpoint.getHistogramValue();
					if (histogramValueExpression != null) {
						Long value = histogramValueExpression.evaluate(exchange, Long.class);
						if (value != null) {
							Histogram histogram = this.endpoint.getHistogram();
							if (histogram != null) {
								histogram.update(value);
							}
						} else {
							LOGGER.warn(MARKER, "histogramValue does not evaluate to a Long");
						}
					}
					break;
				case STOP:
					Map<String, Context> contextMapStop = getTimerContextMap(exchange);
					if (contextMapStop != null) {
						// stop previous context if it exists, and remove it from the map
						Context context = contextMapStop.remove(this.endpoint.getName());
						if (context != null) {
							context.stop();
						}
					} else {
						LOGGER.warn(MARKER, "contextMap is null, timing will not be recorded correctly");
					}
					break;
			}
		} finally {
			if (internalContext != null) {
				internalContext.stop();
			}
		}

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
