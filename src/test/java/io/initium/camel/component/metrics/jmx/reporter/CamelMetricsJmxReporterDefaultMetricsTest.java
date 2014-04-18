package io.initium.camel.component.metrics.jmx.reporter;

import static org.hamcrest.CoreMatchers.equalTo;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;


public class CamelMetricsJmxReporterDefaultMetricsTest extends CamelMetricsJmxReporterTestSupport {

	@EndpointInject(uri = "mock:resultOne")
	protected MockEndpoint resultEndpointOne;

	@Produce(uri = "direct:startOne")
	protected ProducerTemplate templateOne;
	
	@EndpointInject(uri = "mock:resultTwo")
	protected MockEndpoint resultEndpointTwo;

	@Produce(uri = "direct:startTwo")
	protected ProducerTemplate templateTwo;
	
	@Test
	public void testDefaultJmx() {
		templateOne.sendBody("test");
		
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.intervalHours"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.intervalMinutes"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.intervalSeconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.intervalMilliseconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.sinceHours"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.sinceMinutes"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.sinceSeconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.sinceMilliseconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.rate"), equalTo(true));
		
		assertThat(verifyAttributeValueLong("metrics:name=test.rate", "Count", 1L), equalTo(true));
		
		resultEndpointOne.expectedMessageCount(1);
	}
	
	@Test
	public void testDefaultWithDomainJmx() {
		templateTwo.sendBody("test");
		
		assertThat(verifyObjectNameIsRegistered("myTestDomain:name=test2.intervalHours"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("myTestDomain:name=test2.intervalMinutes"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("myTestDomain:name=test2.intervalSeconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("myTestDomain:name=test2.intervalMilliseconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("myTestDomain:name=test2.sinceHours"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("myTestDomain:name=test2.sinceMinutes"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("myTestDomain:name=test2.sinceSeconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("myTestDomain:name=test2.sinceMilliseconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("myTestDomain:name=test2.rate"), equalTo(true));
		
		assertThat(verifyAttributeValueLong("myTestDomain:name=test2.rate", "Count", 1L), equalTo(true));
		
		resultEndpointTwo.expectedMessageCount(1);
	}
	
	@Test 
	public void testRateDecreases() throws InterruptedException {
		templateOne.sendBody("test");
		templateOne.sendBody("test");
		templateOne.sendBody("test");
		
		Double rate1 = getValue("metrics:name=test.rate", "MeanRate", Double.class);
		Thread.sleep(2000);
		Double rate2 = getValue("metrics:name=test.rate", "MeanRate", Double.class);
		
		assertThat(rate2 < rate1, equalTo(true));
	}
	
	@Override
	protected RouteBuilder createRouteBuilder() {
		return new RouteBuilder() {
			@Override
			public void configure() {				
				from("direct:startOne").to("metrics://test?jmxReporters=[{}]").to("mock:resultOne");
				from("direct:startTwo").to("metrics://test2?jmxReporters=[{domain:myTestDomain}]").to("mock:resultTwo");
			}
		};
	}
}
