package io.initium.camel.component.metrics.jmx.reporter;

import static org.hamcrest.CoreMatchers.equalTo;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;


public class CamelMetricsJmxReporterTimingTest extends CamelMetricsJmxReporterTestSupport {

	@EndpointInject(uri = "mock:resultOne")
	protected MockEndpoint resultEndpointOne;

	@Produce(uri = "direct:startOne")
	protected ProducerTemplate templateOne;
	
	@EndpointInject(uri = "mock:resultTwo")
	protected MockEndpoint resultEndpointTwo;

	@Produce(uri = "direct:startTwo")
	protected ProducerTemplate templateTwo;
	
	@Test
	public void testTimingRoute() {
		templateOne.sendBody("test");
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.timing"), equalTo(true));
		resultEndpointOne.expectedMessageCount(1);
		assertThat(getValue("metrics:name=test.timing", "Mean", Double.class) > 0, equalTo(true));
	}
	
	@Test
	public void testVerifyTimingUnit() {
		templateOne.sendBody("test");
		assertThat(getValue("metrics:name=testUnits.timing", "RateUnit", String.class), equalTo("events/minute"));
		assertThat(getValue("metrics:name=testUnits.timing", "DurationUnit", String.class), equalTo("hours"));
	}
	
	@Override
	protected RouteBuilder createRouteBuilder() {
		return new RouteBuilder() {
			@Override
			public void configure() {				
				from("direct:startOne").to("metrics://test?timing=start&jmxReporters=[{}]").delay(2000).to("metrics://test?timing=stop").to("mock:resultOne");
				from("direct:startTwo").to("metrics://testUnits?timing=start&jmxReporters=[{rateUnit=MINUTES,durationUnit=HOURS}]").delay(2000).to("metrics://test?timing=stop").to("mock:resultTwo");
			}
		};
	}
}