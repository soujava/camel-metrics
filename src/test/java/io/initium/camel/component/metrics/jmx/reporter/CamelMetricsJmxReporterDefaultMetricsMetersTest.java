package io.initium.camel.component.metrics.jmx.reporter;

import static org.hamcrest.CoreMatchers.equalTo;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;


public class CamelMetricsJmxReporterDefaultMetricsMetersTest extends CamelMetricsJmxReporterTestSupport {

	@EndpointInject(uri = "mock:resultOne")
	protected MockEndpoint resultEndpointOne;

	@Produce(uri = "direct:startOne")
	protected ProducerTemplate templateOne;
	
	@EndpointInject(uri = "mock:resultTwo")
	protected MockEndpoint resultEndpointTwo;

	@Produce(uri = "direct:startTwo")
	protected ProducerTemplate templateTwo;
	
	@Test
	public void testMeterJmx() {
		templateOne.sendBody("test");
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.myMeter"), equalTo(true));
		assertThat(verifyAttributeValueLong("metrics:name=test.myCounter", "Count", 1L), equalTo(true));
		resultEndpointOne.expectedMessageCount(1);
	}
	
	@Test
	public void testMultipleMeterJmx() {
		templateTwo.sendBody("test");
		assertThat(verifyObjectNameIsRegistered("metrics:name=test2.myMeter2"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test2.myMeter3"), equalTo(true));
		assertThat(verifyAttributeValueLong("metrics:name=test2.myMeter2", "Count", 2L), equalTo(true));
		assertThat(verifyAttributeValueLong("metrics:name=test2.myMeter3", "Count", 3L), equalTo(true));
		resultEndpointTwo.expectedMessageCount(1);
	}
		
	@Override
	protected RouteBuilder createRouteBuilder() {
		return new RouteBuilder() {
			@Override
			public void configure() {				
				from("direct:startOne").to("metrics://test?jmxReporters=[{}]&meter={value=1,name=myMeter}").to("mock:resultOne");
				from("direct:startTwo").to("metrics://test2?jmxReporters=[{}]&meters=[{value=2,name=myMeter2},{value=3,name=myMeter3}]").to("mock:resultTwo");
			}
		};
	}
}
