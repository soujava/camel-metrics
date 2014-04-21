package io.initium.camel.component.metrics.jmx.reporter;

import static org.hamcrest.CoreMatchers.equalTo;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;


public class CamelMetricsJmxReporterDefaultMetricsHistogramsTest extends CamelMetricsJmxReporterTestSupport {

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
	public void testHistogramDefaultJmx() {
		templateOne.sendBody("test");
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.myHistogram"), equalTo(true));
		assertThat(verifyAttributeValueLong("metrics:name=test.rate", "Count", 1L), equalTo(true));
		assertThat(verifyAttributeValueLong("metrics:name=test.myHistogram", "Max", 1L), equalTo(true));
		resultEndpointOne.expectedMessageCount(1);
	}
	
	@Test
	public void testMultipleHistogramsJmx() {
		templateTwo.sendBody("test");
		assertThat(verifyObjectNameIsRegistered("metrics:name=test2.myHistogram2"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test2.myHistogram3"), equalTo(true));
		assertThat(verifyAttributeValueLong("metrics:name=test2.myHistogram2", "Max", 2L), equalTo(true));
		assertThat(verifyAttributeValueLong("metrics:name=test2.myHistogram3", "Max", 3L), equalTo(true));
		resultEndpointTwo.expectedMessageCount(1);
	}
	
	
	@Test
	public void testSimpleHistogramDefaultJmx() {
		templateThree.sendBodyAndHeader("test", "size", 100);	
		assertThat(verifyObjectNameIsRegistered("metrics:name=test3.mySimpleHistogram"), equalTo(true));
		assertThat(verifyAttributeValueLong("metrics:name=test3.mySimpleHistogram", "Max", 100L), equalTo(true));
		resultEndpointOne.expectedMessageCount(1);
	}
		
	@Override
	protected RouteBuilder createRouteBuilder() {
		return new RouteBuilder() {
			@Override
			public void configure() {				
				from("direct:startOne").to("metrics://test?jmxReporters=[{}]&histogram={value=1,name=myHistogram}").to("mock:resultOne");
				from("direct:startTwo").to("metrics://test2?jmxReporters=[{}]&histograms=[{value=2,name=myHistogram2},{value=3,name=myHistogram3}]").to("mock:resultTwo");
				from("direct:startThree").to("metrics://test3?jmxReporters=[{}]&histogram={value='${header.size}',name=mySimpleHistogram}").to("mock:resultThree");
			}
		};
	}
}
