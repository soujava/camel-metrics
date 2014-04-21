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

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Ignore;
import org.junit.Test;

public class MetricsTest extends CamelTestSupport {

	@Override
	public boolean isUseRouteBuilder() {
		return false;
	}

	@Test
	@Ignore
	public void theThirdTest() throws Exception {
		// JmxReporterDefinition jmxReporterDefinition = new JmxReporterDefinition();
		// jmxReporterDefinition.setDomain("testDomain");
		// jmxReporterDefinition.setFilter("^(myMetric01.rate|myMetric01.intervalSeconds)$");
		// ConsoleReporterDefinition consoleReporterDefinition = new ConsoleReporterDefinition();
		// consoleReporterDefinition.setPeriodDuration(1);
		// consoleReporterDefinition.setPeriodDurationUnit(TimeUnit.SECONDS);
		// MetricsComponent metricsComponent = new MetricsComponent(jmxReporterDefinition);
		// this.context.addComponent("metrics", metricsComponent);
		this.context.addRoutes(new RouteBuilder() {
			private final Random	random	= new Random();

			@Override
			public void configure() throws Exception {
				final List<String> PROVIDERS = Arrays.asList("ADAC", "GARMIN", "NAVIGON");
				final List<String> PROVIDERS_BODY = Arrays.asList("#", "##", "###");
				Processor myProcessor = new Processor() {
					@Override
					public void process(final Exchange exchange) throws Exception {
						int index = random.nextInt(PROVIDERS.size());
						exchange.getIn().setHeader("providerName", PROVIDERS.get(index));
						exchange.getIn().setBody(PROVIDERS_BODY.get(index));
					}
				};

				// @formatter:off
				from("timer://myTestTimer?period=100")

					// simple example to console
					//.to("metrics://yourFirstMetric?consoleReporter={periodDurationUnit=seconds,periodDuration=1}")

					// custom meter example
					//.to("metrics://test?jmxReporters=[{}]&meter={value=1,name=myMeter}")
				
					// custom gauge example
					//.to("metrics://test?jmxReporters=[{}]&gauge={value=1,name=myGauge}")

					// custom cached gauge example
					//.process(myProcessor)
					//.to("metrics://test?jmxReporters=[{}]&cachedGauge={value='${header.providerName}',name=mySimpleCachedGauge,duration=10,durationUnit=seconds}")

					// simple timing example
					//.to("metrics://yourFirstMetric?timing=start&consoleReporter={periodDurationUnit=seconds,periodDuration=1}")
					//.delay(1000)
					//.to("metrics://yourFirstMetric?timing=stop&consoleReporter={periodDurationUnit=seconds,periodDuration=1}")

					// infix example
					.process(myProcessor)
					.to("metrics://requests?infix=['${header.providerName}','X${header.providerName}']&jmxReporters=[{}]")

					// miscellaneous examples
					//.to("metrics://myMetric01?timing=start&infix=${in.body}")
					//.to("metrics://yourFirstMetric?sinceTimeUnits=[SECONDS,DAYS]&consoleReporter={periodDurationUnit=seconds,periodDuration=1}")
					//.to("metrics://yourFirstMetric?histograms=[{name=One,value=2},{value=3}]&consoleReporter={periodDurationUnit=seconds,periodDuration=1}")
					//.to("metrics://yourFirstMetric?jmxReporter={domain=metrics1}")
					//.to("metrics://yourSecondMetric?jmxReporter={domain=metrics2}")
					//.to("metrics://yourSecondMetric?jmxReporter={domain=metrics2}")
					//.to("metrics://myMetric01?enableInternalTimer=true")
					//.delay(1000)
					//.to("metrics://myMetric01?timing=stop&infix=${in.body}")
					//.to("metrics://myMetric01?jmxReporters=[{domain:testReplacedDomain}]")
					;
				// @formatter:on
			}
		});
		this.context.start();
		TimeUnit.SECONDS.sleep(200);
		this.context.stop();
	}
}
