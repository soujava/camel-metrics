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
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

import static io.initium.camel.component.metrics.MetricsComponent.MARKER;
import static io.initium.camel.component.metrics.MetricsComponent.TIMING_MAP_NAME;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.0
 * @since 2014-02-19
 */
public class MetricsProducer extends DefaultProducer {

	// logging
	private static final String	SELF	= Thread.currentThread().getStackTrace()[1].getClassName();
	private static final Logger	LOGGER	= LoggerFactory.getLogger(SELF);

	/**
	 * @return
	 */
	private static String getFullTimingName(final String metricName, final String timingName) {
		return MetricRegistry.name(metricName, timingName);
	}

	/**
	 * @param exchange
	 * @return
	 */
	private static Map<String, Context> getTimerContextMap(final Exchange exchange) {
		@SuppressWarnings("unchecked")
		Map<String, Context> timerContextMap = exchange.getProperty(TIMING_MAP_NAME, Map.class);
		if (timerContextMap != null) {
			return timerContextMap;
		}
		timerContextMap = new HashMap<String, Context>();
		exchange.setProperty(TIMING_MAP_NAME, timerContextMap);
		return timerContextMap;
	}

	// fields
	private final MetricsEndpoint	endpoint;
	private boolean					haveProcessedAtLeastOneExchange	= false;

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

	@Override
	public void process(final Exchange exchange) throws Exception {
		LOGGER.debug(MARKER, "process({})", exchange);
		if (this.haveProcessedAtLeastOneExchange) {
			this.haveProcessedAtLeastOneExchange = true;
			Context internalContext = null;
			if (this.endpoint.isInternalTimerEnabled()) {
				Timer internalTimer = this.endpoint.getInternalTimer();
				if (internalTimer != null) {
					internalContext = internalTimer.time();
				}
			}
			try {
				switch (this.endpoint.getTimingAction()) {
					case START:
						startTimer(exchange);
					case NOOP:
						// set lastExchange in endpoint for optional gauge
						this.endpoint.mark(exchange);
						addOptionalCounter(exchange);
						addOptionalHistogram(exchange);
						break;
					case STOP:
						stopTimer(exchange);
						break;
				}
			} finally {
				if (internalContext != null) {
					internalContext.stop();
				}
			}
		}
	}

	/**
	 * @param exchange
	 */
	private void addOptionalCounter(final Exchange exchange) {
		// optional Counter
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
	}

	/**
	 * @param exchange
	 */
	private void addOptionalHistogram(final Exchange exchange) {
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
	}

	/**
	 * Starts the Timer context, but first Stops the previous, samely named, Timer context if it exists.
	 * 
	 * @param exchange
	 */
	private void startTimer(final Exchange exchange) {
		Map<String, Context> map = getTimerContextMap(exchange);
		if (map != null) {
			// stop previous context if it exists
			Context context = map.get(getFullTimingName(this.endpoint.getName(), this.endpoint.getTimingName()));
			if (context != null) {
				context.stop();
			}
			// start new context
			context = this.endpoint.getTimer().time();
			map.put(this.endpoint.getName(), context);
		} else {
			LOGGER.warn(MARKER, "contextMap is null, timing will not be recorded correctly");
		}
	}

	/**
	 * Stops the Timer context if it exists, and removes it from the map in the exchange.
	 * 
	 * @param exchange
	 */
	private void stopTimer(final Exchange exchange) {
		Map<String, Context> map = getTimerContextMap(exchange);
		if (map != null) {
			Context context = map.remove(this.endpoint.getName());
			if (context != null) {
				context.stop();
			}
		} else {
			LOGGER.warn(MARKER, "contextMap is null, timing will not be recorded correctly");
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
