package io.initium.camel.component.metrics.jmx.reporter;

import static org.hamcrest.CoreMatchers.equalTo;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;


public class CamelMetricsJmxReporterDefaultMetricsCounterTest extends CamelMetricsJmxReporterTestSupport {

	@EndpointInject(uri = "mock:resultOne")
	protected MockEndpoint resultEndpointOne;

	@Produce(uri = "direct:startOne")
	protected ProducerTemplate templateOne;
	
	@EndpointInject(uri = "mock:resultTwo")
	protected MockEndpoint resultEndpointTwo;

	@Produce(uri = "direct:startTwo")
	protected ProducerTemplate templateTwo;
	
	@Test
	public void testCounterJmx() {
		templateOne.sendBody("test");
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.myCounter"), equalTo(true));
		assertThat(verifyAttributeValueLong("metrics:name=test.myCounter", "Count", 1L), equalTo(true));
		resultEndpointOne.expectedMessageCount(1);
	}
	
	@Test
	public void testMultipleCountersJmx() {
		templateTwo.sendBody("test");
		assertThat(verifyObjectNameIsRegistered("metrics:name=test2.myCounter2"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test2.myCounter3"), equalTo(true));
		assertThat(verifyAttributeValueLong("metrics:name=test2.myCounter2", "Count", 2L), equalTo(true));
		assertThat(verifyAttributeValueLong("metrics:name=test2.myCounter3", "Count", 3L), equalTo(true));
		resultEndpointTwo.expectedMessageCount(1);
	}
		
	@Override
	protected RouteBuilder createRouteBuilder() {
		return new RouteBuilder() {
			@Override
			public void configure() {				
				from("direct:startOne").to("metrics://test?jmxReporters=[{}]&counter={value=1,name=myCounter}").to("mock:resultOne");
				from("direct:startTwo").to("metrics://test2?jmxReporters=[{}]&counters=[{value=2,name=myCounter2},{value=3,name=myCounter3}]").to("mock:resultTwo");
			}
		};
	}
}
