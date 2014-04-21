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

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;

public class OptionHelperTest {

	@Test
	public void parseForBooleanFalse() {
		String[] values = {"false", "FALSE", "fAlse", "0", "no"};
		for (String value : values) {
			assertThat(OptionHelper.parse(value, Boolean.class), equalTo(false));
		}
	}

	@Test
	public void parseForBooleanTrue() {
		String[] values = {"true", "TRUE", "tRue", "1", "yes"};
		for (String value : values) {
			assertThat(OptionHelper.parse(value, Boolean.class), equalTo(true));
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void parseForList() {
		String[] values = {"[A,B]", "A,B", "'A','B'", "['A','B']", "[\"A\",\"B\"]", "[\"A\",B]"};
		for (String value : values) {
			List<String> result = OptionHelper.parse(value, List.class);
			assertThat("type, value=" + value, result, isA(List.class));
			assertThat("size, value=" + value, result.size(), equalTo(2));
			assertThat("get(0), value=" + value, result.get(0), equalTo("A"));
			assertThat("get(1), value=" + value, result.get(1), equalTo("B"));
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void parseForListCommas() {
		String[] values = {"['A,',B]", "\"A,\",B"};
		for (String value : values) {
			List<String> result = OptionHelper.parse(value, List.class);
			assertThat("type, value=" + value, result, isA(List.class));
			assertThat("size, value=" + value, result.size(), equalTo(2));
			assertThat("get(0), value=" + value, result.get(0), equalTo("A,"));
			assertThat("get(1), value=" + value, result.get(1), equalTo("B"));
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void parseForSparseList() {
		{
			String value = "[1,2,3,STEVE]";
			List<Double> result = OptionHelper.parse(value, List.class);
			assertThat("type, value=" + value, result, isA(List.class));
			// assertThat("size, value=" + value, result.size(), equalTo(3));
			assertThat("get(0), value=" + value, result.get(0), equalTo(1D));
			assertThat("get(1), value=" + value, result.get(1), equalTo(2D));
			assertThat("get(2), value=" + value, result.get(2), equalTo(3D));
		}
		{
			String value = "[A,,C]";
			List<String> result = OptionHelper.parse(value, List.class);
			assertThat("type, value=" + value, result, isA(List.class));
			assertThat("size, value=" + value, result.size(), equalTo(3));
			assertThat("get(0), value=" + value, result.get(0), equalTo("A"));
			assertThat("get(1), value=" + value, result.get(1), equalTo(null));
			assertThat("get(2), value=" + value, result.get(2), equalTo("C"));
		}
		{
			String value = "[A,B,]";
			List<String> result = OptionHelper.parse(value, List.class);
			assertThat("type, value=" + value, result, isA(List.class));
			assertThat("size, value=" + value, result.size(), equalTo(3));
			assertThat("get(0), value=" + value, result.get(0), equalTo("A"));
			assertThat("get(1), value=" + value, result.get(1), equalTo("B"));
			assertThat("get(2), value=" + value, result.get(2), equalTo(null));
		}
		{
			String value = "[,B,C]";
			List<String> result = OptionHelper.parse(value, List.class);
			assertThat("type, value=" + value, result, isA(List.class));
			assertThat("size, value=" + value, result.size(), equalTo(3));
			assertThat("get(0), value=" + value, result.get(0), equalTo(null));
			assertThat("get(1), value=" + value, result.get(1), equalTo("B"));
			assertThat("get(2), value=" + value, result.get(2), equalTo("C"));
		}
	}

	@Test
	public void parseForTimeUnitMinutes() {
		String[] values = {"minute", "minutes", "MINUTE", "MINUTES", "mInute", "miNutes"};
		for (String value : values) {
			assertThat(OptionHelper.parse(value, TimeUnit.class), equalTo(TimeUnit.MINUTES));
		}
	}

	@Test
	public void parseForTimeUnitSeconds() {
		String[] values = {"second", "seconds", "SECOND", "SECONDS", "secOnd", "seCOnds"};
		for (String value : values) {
			assertThat(OptionHelper.parse(value, TimeUnit.class), equalTo(TimeUnit.SECONDS));
		}
	}

}
