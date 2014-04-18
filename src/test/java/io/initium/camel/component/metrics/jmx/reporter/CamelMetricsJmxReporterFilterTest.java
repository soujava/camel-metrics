package io.initium.camel.component.metrics.jmx.reporter;

import static org.hamcrest.CoreMatchers.equalTo;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;


public class CamelMetricsJmxReporterFilterTest extends CamelMetricsJmxReporterTestSupport {

	@EndpointInject(uri = "mock:resultOne")
	protected MockEndpoint resultEndpointOne;

	@Produce(uri = "direct:startOne")
	protected ProducerTemplate templateOne;
	
	@Test
	public void testDefaultJmx() {
		templateOne.sendBody("test");
		
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.intervalHours"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.intervalMinutes"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.intervalSeconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.intervalMilliseconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.sinceHours"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.sinceMinutes"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.sinceSeconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.sinceMilliseconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.rate"), equalTo(true));
		
		assertThat(verifyAttributeValueLong("metrics:name=test.rate", "Count", 1L), equalTo(true));
		
		resultEndpointOne.expectedMessageCount(1);
	}
	
	@Override
	protected RouteBuilder createRouteBuilder() {
		return new RouteBuilder() {
			@Override
			public void configure() {				
				from("direct:startOne").to("metrics://test?jmxReporters=[{filter=^(.*.rate)$}]").to("mock:resultOne");
			}
		};
	}
}
