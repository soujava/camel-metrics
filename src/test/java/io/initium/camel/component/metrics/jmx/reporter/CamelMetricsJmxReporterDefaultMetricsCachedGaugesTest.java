package io.initium.camel.component.metrics.jmx.reporter;

import static org.hamcrest.CoreMatchers.equalTo;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;


public class CamelMetricsJmxReporterDefaultMetricsCachedGaugesTest extends CamelMetricsJmxReporterTestSupport {

	@EndpointInject(uri = "mock:resultOne")
	protected MockEndpoint resultEndpointOne;

	@Produce(uri = "direct:startOne")
	protected ProducerTemplate templateOne;
	
	@EndpointInject(uri = "mock:resultTwo")
	protected MockEndpoint resultEndpointTwo;

	@Produce(uri = "direct:startTwo")
	protected ProducerTemplate templateTwo;
	
	@EndpointInject(uri = "mock:resultThree")
	protected MockEndpoint resultEndpointThree;

	@Produce(uri = "direct:startThree")
	protected ProducerTemplate templateThree;
	
	@Test
	public void testCachedGaugesDefaultJmx() {
		templateOne.sendBody("test");		
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.myGauge"), equalTo(true));
		assertThat(verifyAttributeValueLong("metrics:name=test.rate", "Count", 1L), equalTo(true));
		assertThat(verifyAttributeValueLong("metrics:name=test.myGauge", "Value", 1L), equalTo(true));
		resultEndpointOne.expectedMessageCount(1);
	}
	
	@Test
	public void testMulitpleCachedGaugesJmx() {
		templateTwo.sendBody("test");
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.myGauge2"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.myGauge3"), equalTo(true));
		assertThat(verifyAttributeValueLong("metrics:name=test.myGauge2", "Value", 2L), equalTo(true));
		assertThat(verifyAttributeValueLong("metrics:name=test.myGauge3", "Value", 3L), equalTo(true));
		resultEndpointTwo.expectedMessageCount(1);
	}
	
	@Test
	public void testSimpleCachedGaugeDefaultJmx() {
		templateThree.sendBodyAndHeader("test", "size", 100);	
		assertThat(verifyObjectNameIsRegistered("metrics:name=test3.mySimpleCachedGauge"), equalTo(true));
		assertThat(verifyAttributeValueLong("metrics:name=test3.mySimpleCachedGauge", "Count", 100L), equalTo(true));
		resultEndpointOne.expectedMessageCount(1);
	}
		
	@Override
	protected RouteBuilder createRouteBuilder() {
		return new RouteBuilder() {
			@Override
			public void configure() {				
				from("direct:startOne").to("metrics://test?jmxReporters=[{}]&cachedGauge={value=1,name=myGauge,duration=1,durationUnit=minutes}").to("mock:resultOne");
				from("direct:startTwo").to("metrics://test2?jmxReporters=[{}]&cachedGauges=[{value=2,name=myGauge2,duration=1,durationUnit=minutes},{value=3,name=myGauge3,duration=1,durationUnit=minutes}]").to("mock:resultTwo");
				from("direct:startThree").to("metrics://test3?jmxReporters=[{}]&cachedGauge={value=1,name=mySimpleCachedGauge,duration=1,durationUnit=minutes}").to("mock:resultThree");
			}
		};
	}
}
