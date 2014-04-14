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
package io.initium.camel.component.metrics.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.Timer;

import io.initium.camel.component.metrics.MetricsComponent;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.1
 * @since 2014-04-08
 */
public class LoggingMetricRegistryListener implements MetricRegistryListener {

	/**
	 * Supported logging levels.
	 */
	public static enum Level {
		TRACE,
		DEBUG,
		INFO,
		WARN,
		ERROR
	}

	// constants
	private static final String	SELF			= Thread.currentThread().getStackTrace()[1].getClassName();
	private static final Logger	DEFAULT_LOGGER	= LoggerFactory.getLogger(SELF);
	private static final Marker	DEFAULT_MARKER	= MetricsComponent.MARKER;
	private static final Level	DEFAULT_LEVEL	= Level.DEBUG;

	// fields
	private final Logger		logger;
	public Marker				marker;
	public Level				level;

	/**
	 * 
	 */
	public LoggingMetricRegistryListener() {
		this(DEFAULT_LOGGER, DEFAULT_MARKER, DEFAULT_LEVEL);
	}

	/**
	 * @param logger
	 */
	public LoggingMetricRegistryListener(final Logger logger) {
		this(logger, DEFAULT_MARKER, DEFAULT_LEVEL);
	}

	/**
	 * @param logger
	 * @param level
	 */
	public LoggingMetricRegistryListener(final Logger logger, final Level level) {
		this(logger, DEFAULT_MARKER, level);
	}

	/**
	 * @param logger
	 * @param marker
	 */
	public LoggingMetricRegistryListener(final Logger logger, final Marker marker) {
		this(logger, marker, DEFAULT_LEVEL);
	}

	/**
	 * @param logger
	 * @param marker
	 * @param level
	 */
	public LoggingMetricRegistryListener(final Logger logger, final Marker marker, final Level level) {
		this.logger = logger;
		this.marker = marker;
		this.level = level;
	}

	@Override
	public void onCounterAdded(final String name, final Counter counter) {
		log("counter added: {}", name);
	}

	@Override
	public void onCounterRemoved(final String name) {
		log("counter removed: {}", name);
	}

	@Override
	public void onGaugeAdded(final String name, final Gauge<?> gauge) {
		log("gauge added: {}", name);
	}

	@Override
	public void onGaugeRemoved(final String name) {
		log("gauge removed: {}", name);
	}

	@Override
	public void onHistogramAdded(final String name, final Histogram histogram) {
		log("histogram added: {}", name);
	}

	@Override
	public void onHistogramRemoved(final String name) {
		log("histogram removed: {}", name);
	}

	@Override
	public void onMeterAdded(final String name, final Meter meter) {
		log("meter added: {}", name);
	}

	@Override
	public void onMeterRemoved(final String name) {
		log("meter removed: {}", name);
	}

	@Override
	public void onTimerAdded(final String name, final Timer timer) {
		log("timer added: {}", name);
	}

	@Override
	public void onTimerRemoved(final String name) {
		log("timer removed: {}", name);
	}

	/**
	 * @param format
	 * @param arguments
	 */
	private void log(final String format, final Object... arguments) {
		switch (this.level) {
			case TRACE:
				if (this.marker != null) {
					this.logger.trace(this.marker, format, arguments);
				} else {
					this.logger.trace(format, arguments);
				}
				break;
			case DEBUG:
				if (this.marker != null) {
					this.logger.debug(this.marker, format, arguments);
				} else {
					this.logger.debug(format, arguments);
				}
				break;
			case INFO:
				if (this.marker != null) {
					this.logger.info(this.marker, format, arguments);
				} else {
					this.logger.info(format, arguments);
				}
				break;
			case WARN:
				if (this.marker != null) {
					this.logger.warn(this.marker, format, arguments);
				} else {
					this.logger.warn(format, arguments);
				}
				break;
			case ERROR:
				if (this.marker != null) {
					this.logger.error(this.marker, format, arguments);
				} else {
					this.logger.error(format, arguments);
				}
				break;
		}
	}

}
