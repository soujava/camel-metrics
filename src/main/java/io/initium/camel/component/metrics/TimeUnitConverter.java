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

import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * @author Steve Fosdal, <steve@initium.io>
 * @author Hector Veiga Ortiz, <hector@initium.io>
 * @version 1.0
 * @since 2014-02-19
 */
public class TimeUnitConverter implements JsonSerializer<TimeUnit>, JsonDeserializer<TimeUnit> {

	@Override
	public TimeUnit deserialize(final JsonElement json, final Type type, final JsonDeserializationContext context) throws JsonParseException {
		TimeUnit timeUnit = null;
		try {
			timeUnit = TimeUnit.valueOf(json.getAsString().toUpperCase());
		} catch (Exception e) {
			timeUnit = TimeUnit.valueOf(json.getAsString().toUpperCase() + "S");
		}
		return timeUnit;
	}

	@Override
	public JsonElement serialize(final TimeUnit src, final Type srcType, final JsonSerializationContext context) {
		return new JsonPrimitive(src.toString());
	}
}
