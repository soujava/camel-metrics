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

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	/**
	 * @param endpoint
	 */
	public MetricsProducer(final MetricsEndpoint endpoint) {
		super(endpoint);
		LOGGER.debug(MARKER, "MetricsProducer({})", endpoint);
		this.endpoint = endpoint;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void process(final Exchange exchange) throws Exception {
		LOGGER.debug(MARKER, "process({})", exchange);
		Context internalTimerContext = null;
		if (this.endpoint.isInternalTimerEnabled()) {
			Timer internalTimer = this.endpoint.getInternalTimer(this.endpoint.getTimingAction());
			if (internalTimer != null) {
				internalTimerContext = internalTimer.time();
			}
		}
		try {
			MetricGroup standardMetricGroup = this.endpoint.lookupMetricGroup(this.endpoint.getName());
			switch (this.endpoint.getTimingAction()) {
				case START:
					startTimer(standardMetricGroup, exchange);
				case NOOP:
					standardMetricGroup.mark(exchange);
					break;
				case STOP:
					stopTimer(exchange);
					break;
			}
			// determine non-standard MetricGroup
			MetricGroup dynamicMetricGroup = null;
			String infixValue = null;
			if (this.endpoint.getInfixExpression() != null) {
				infixValue = this.endpoint.getInfixExpression().evaluate(exchange, String.class);
				String fullMetricGroupName = MetricRegistry.name(this.endpoint.getName(), infixValue);
				if (!fullMetricGroupName.equalsIgnoreCase(MetricRegistry.name(this.endpoint.getName()))) {
					dynamicMetricGroup = this.endpoint.lookupMetricGroup(this.endpoint.getName(), infixValue, exchange);
				}
			}
			if (dynamicMetricGroup != null) {
				switch (this.endpoint.getTimingAction()) {
					case START:
						startTimer(dynamicMetricGroup, infixValue, exchange);
					case NOOP:
						dynamicMetricGroup.mark(exchange);
						break;
					case STOP:
						stopTimer(infixValue, exchange);
						break;
				}
			}
		} finally {
			if (internalTimerContext != null) {
				internalTimerContext.stop();
			}
		}
	}

	/**
	 * @param metricGroup
	 * @param exchange
	 */
	private void startTimer(final MetricGroup metricGroup, final Exchange exchange) {
		startTimer(metricGroup, null, exchange);
	}

	private void startTimer(final MetricGroup metricGroup, final String infixValue, final Exchange exchange) {
		Map<String, Context> timerContextMap = getTimerContextMap(exchange);
		if (timerContextMap != null) {
			String fullTimerName;
			if (infixValue != null) {
				fullTimerName = MetricRegistry.name(this.endpoint.getName(), infixValue, this.endpoint.getTimingName());
			} else {
				fullTimerName = MetricRegistry.name(this.endpoint.getName(), this.endpoint.getTimingName());
			}
			// stop previous context if it exists
			Context timerContext = timerContextMap.get(fullTimerName);
			if (timerContext != null) {
				timerContext.stop();
			}
			// start new context
			timerContext = metricGroup.getTimer().time();
			timerContextMap.put(fullTimerName, timerContext);
		} else {
			LOGGER.warn(MARKER, "timerContextMap is null, timing will not be recorded correctly");
		}
	}

	/**
	 * @param exchange
	 */
	private void stopTimer(final Exchange exchange) {
		stopTimer(null, exchange);
	}

	/**
	 * @param infixValue
	 * @param exchange
	 */
	private void stopTimer(final String infixValue, final Exchange exchange) {
		Map<String, Context> timerContextMap = getTimerContextMap(exchange);
		if (timerContextMap != null) {
			String fullTimerName;
			if (infixValue != null) {
				fullTimerName = MetricRegistry.name(this.endpoint.getName(), infixValue, this.endpoint.getTimingName());
			} else {
				fullTimerName = MetricRegistry.name(this.endpoint.getName(), this.endpoint.getTimingName());
			}
			// stop previous context if it exists
			Context timerContext = timerContextMap.get(fullTimerName);
			if (timerContext != null) {
				timerContext.stop();
			}
		} else {
			LOGGER.warn(MARKER, "timerContextMap is null, timing will not be recorded correctly");
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
