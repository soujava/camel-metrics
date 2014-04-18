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
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import io.initium.camel.component.metrics.TimeUnitConverter;
import io.initium.camel.component.metrics.definition.metric.CachedGaugeDefinition;
import io.initium.camel.component.metrics.definition.metric.CounterDefinition;
import io.initium.camel.component.metrics.definition.metric.GaugeDefinition;
import io.initium.camel.component.metrics.definition.metric.HistogramDefinition;
import io.initium.camel.component.metrics.definition.reporter.ConsoleReporterDefinition;
import io.initium.camel.component.metrics.definition.reporter.CsvReporterDefinition;
import io.initium.camel.component.metrics.definition.reporter.GraphiteReporterDefinition;
import io.initium.camel.component.metrics.definition.reporter.JmxReporterDefinition;
import io.initium.camel.component.metrics.definition.reporter.Slf4jReporterDefinition;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.2
 * @since 2014-04-17
 */
public final class GsonHelper {

	private static final String	SELF							= Thread.currentThread().getStackTrace()[1].getClassName();

	public static final Gson	GSON;
	static {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(TimeUnit.class, new TimeUnitConverter());
		// TODO register InstanceCreators for the expression based metrics endpoint options
		GSON = gsonBuilder.create();
	}

	public static final Type	JMX_REPORTERS_TYPE				= new TypeToken<Collection<JmxReporterDefinition>>() {}.getType();
	public static final Type	JMX_REPORTER_TYPE				= new TypeToken<JmxReporterDefinition>() {}.getType();

	public static final Type	CONSOLE_REPORTERS_TYPE			= new TypeToken<Collection<ConsoleReporterDefinition>>() {}.getType();
	public static final Type	CONSOLE_REPORTER_TYPE			= new TypeToken<ConsoleReporterDefinition>() {}.getType();

	public static final Type	GRAPHITE_REPORTERS_TYPE			= new TypeToken<Collection<GraphiteReporterDefinition>>() {}.getType();
	public static final Type	GRAPHITE_REPORTER_TYPE			= new TypeToken<GraphiteReporterDefinition>() {}.getType();

	public static final Type	SLF4J_REPORTERS_TYPE			= new TypeToken<Collection<Slf4jReporterDefinition>>() {}.getType();
	public static final Type	SLF4J_REPORTER_TYPE				= new TypeToken<Slf4jReporterDefinition>() {}.getType();

	public static final Type	CSV_REPORTERS_TYPE				= new TypeToken<Collection<CsvReporterDefinition>>() {}.getType();
	public static final Type	CSV_REPORTER_TYPE				= new TypeToken<CsvReporterDefinition>() {}.getType();

	public static final Type	TIME_UNITS_TYPE					= new TypeToken<Collection<TimeUnit>>() {}.getType();
	public static final Type	TIME_UNIT_TYPE					= new TypeToken<TimeUnit>() {}.getType();

	public static final Type	HISTOGRAM_DEFINITIONS_TYPE		= new TypeToken<Collection<HistogramDefinition>>() {}.getType();
	public static final Type	HISTOGRAM_DEFINITION_TYPE		= new TypeToken<HistogramDefinition>() {}.getType();

	public static final Type	COUNTER_DEFINITIONS_TYPE		= new TypeToken<Collection<CounterDefinition>>() {}.getType();
	public static final Type	COUNTER_DEFINITION_TYPE			= new TypeToken<CounterDefinition>() {}.getType();

	public static final Type	GAUGE_DEFINITIONS_TYPE			= new TypeToken<Collection<GaugeDefinition>>() {}.getType();
	public static final Type	GAUGE_DEFINITION_TYPE			= new TypeToken<GaugeDefinition>() {}.getType();

	public static final Type	CACHED_GAUGE_DEFINITIONS_TYPE	= new TypeToken<Collection<CachedGaugeDefinition>>() {}.getType();
	public static final Type	CACHED_GAUGE_DEFINITION_TYPE	= new TypeToken<CachedGaugeDefinition>() {}.getType();

	/**
	 * This class is not intended to ever be instantiated.
	 */
	private GsonHelper() {
		throw new AbstractMethodError("this class [" + SELF + "] is not intended to ever be instantiated");
	}
}
