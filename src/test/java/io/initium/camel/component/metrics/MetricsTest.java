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

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Ignore;
import org.junit.Test;

import io.initium.camel.component.metrics.reporters.ConsoleReporterDefinition;
import io.initium.camel.component.metrics.reporters.JmxReporterDefinition;

public class MetricsTest extends CamelTestSupport {

	private static Random	random	= new Random();

	@Override
	public boolean isUseRouteBuilder() {
		return false;
	}

	@Test
	@Ignore
	public void theFirstTest() throws Exception {
		this.context.addRoutes(new RouteBuilder() {
			@Override
			public void configure() throws Exception {

				Processor myRandomProcessor = new Processor() {
					@Override
					public void process(final Exchange exchange) throws Exception {
						StringBuilder stringBuilder = new StringBuilder();
						int size = random.nextInt(10);
						for (int i = 1; i <= size; i++) {
							stringBuilder.append(size);
						}
						exchange.getIn().setBody(stringBuilder.toString());
					}
				};
				// from("timer://namedTimer?period=100").to("metrics://sampleWithoutTiming?jmxDomain=context01");
				// from("timer://namedTimer?period=200").to("metrics://sampleWithoutTiming?jmxDomain=context02").delay(1000);
				// from("timer://namedTimer?period=100").to("metrics://sampleWithTiming?jmxDomain=context02&timing=start").delay(1000).to("metrics://sampleWithTiming?timing=stop");
				// from("timer://namedTimer?period=100").to("metrics://sampleWithCounter1?jmxDomain=context03&counterDelta=3&counterName=MyName");
				// from("timer://namedTimer?period=100").setHeader("testCounterDelta",
				// simple("2")).to("metrics://sampleWithCounter2?counterDelta=${in.header.testCounterDelta}");
				// from("timer://namedTimer?period=100").to("metrics://sampleWithHistogram1?histogramValue=3");
				// from("timer://namedTimer?period=100").process(myRandomProcessor).to("metrics://sampleWithHistogram2?histogramValue=${in.body.length()}");
				// from("timer://namedTimer?period=100").to("metrics://sampleWithGauge1?gaugeValue=sdsdds");
				// from("timer://namedTimer?period=100").process(myRandomProcessor).to("metrics://sampleWithGauge2?gaugeValue=${in.body}");
			}
		});
		this.context.start();
		while (true) {
			TimeUnit.MINUTES.sleep(1);
		}
	}

	@Test
	@Ignore
	public void theSecondTest() throws Exception {
		this.context.addRoutes(new RouteBuilder() {
			@Override
			public void configure() throws Exception {
				// from("timer://namedTimer?period=100").to("metrics://sample?jmxDomain=context1&enableInternalTimer=true");
				// from("timer://namedTimer?period=100").to("metrics://sample?jmxDomain=context2&durationUnit=seConds");
				from("timer://myTestTimer?period=100").to("metrics://myMetric01?jmxDomain=domain3a&jmxRateUnit=MINUTES").to("metrics://myMetric02?jmxDomain=comain3b&jmxRateUnit=SECONDS");
				// from("timer://namedTimer?period=1000").to("metrics://sample?jmxDomain=context3&histogramValue=1");
				// from("timer://namedTimer?period=100").to("metrics://sample?jmxDomain=context4&histogramValue=1&histogramReservoir=slidingTimewindow&slidingTimeWindowDuration=10&slidingTimeWindowDurationUnit=seconds");
			}
		});
		this.context.start();
		while (true) {
			TimeUnit.MINUTES.sleep(10);
		}

	}

	@Test
	@Ignore
	public void theThirdTest() throws Exception {
		JmxReporterDefinition jmxReporterDefinition = new JmxReporterDefinition();
		jmxReporterDefinition.setDomain("testDomain");
		jmxReporterDefinition.setFilter("^(myMetric01.rate|myMetric01.intervalSeconds)$");

		ConsoleReporterDefinition consoleReporterDefinition = new ConsoleReporterDefinition();
		consoleReporterDefinition.setPeriodDuration(1);
		consoleReporterDefinition.setPeriodDurationUnit(TimeUnit.SECONDS);
		MetricsComponent metricsComponent = new MetricsComponent(jmxReporterDefinition, consoleReporterDefinition);
		this.context.addComponent("metrics", metricsComponent);
		this.context.addRoutes(new RouteBuilder() {
			@Override
			public void configure() throws Exception {
				// @formatter:off
				from("timer://myTestTimer?period=1000")
					//.to("log://io.initium.metrics?showAll=true&multiline=false")
					.to("metrics://myMetric01?jmxReporters=[{domain:testReplacedDomain}]&timing=stop")
					;
				// @formatter:on
			}
		});
		this.context.start();
		TimeUnit.SECONDS.sleep(200);
		this.context.stop();
	}
}
