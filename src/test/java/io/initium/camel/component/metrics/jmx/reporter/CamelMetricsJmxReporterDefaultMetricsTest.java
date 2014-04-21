package io.initium.camel.component.metrics.jmx.reporter;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;

public class CamelMetricsJmxReporterDefaultMetricsTest extends CamelMetricsJmxReporterTestSupport {

	@EndpointInject(uri = "mock:resultOne")
	protected MockEndpoint		resultEndpointOne;

	@Produce(uri = "direct:startOne")
	protected ProducerTemplate	templateOne;

	@EndpointInject(uri = "mock:resultTwo")
	protected MockEndpoint		resultEndpointTwo;

	@Produce(uri = "direct:startTwo")
	protected ProducerTemplate	templateTwo;

	@Test
	public void testDefaultJmx() {
		this.templateOne.sendBody("test");

		assertThat(verifyObjectNameIsRegistered("metrics:name=test.interval.hours"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.interval.minutes"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.interval.seconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.interval.milliseconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.since.hours"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.since.minutes"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.since.seconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.since.milliseconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.rate"), equalTo(true));

		assertThat(verifyAttributeValueLong("metrics:name=test.rate", "Count", 1L), equalTo(true));

		this.resultEndpointOne.expectedMessageCount(1);
	}

	@Test
	public void testDefaultWithDomainJmx() {
		this.templateTwo.sendBody("test");

		assertThat(verifyObjectNameIsRegistered("myTestDomain:name=test2.interval.hours"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("myTestDomain:name=test2.interval.minutes"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("myTestDomain:name=test2.interval.seconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("myTestDomain:name=test2.interval.milliseconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("myTestDomain:name=test2.since.hours"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("myTestDomain:name=test2.since.minutes"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("myTestDomain:name=test2.since.seconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("myTestDomain:name=test2.since.milliseconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("myTestDomain:name=test2.rate"), equalTo(true));

		assertThat(verifyAttributeValueLong("myTestDomain:name=test2.rate", "Count", 1L), equalTo(true));

		this.resultEndpointTwo.expectedMessageCount(1);
	}

	@Test
	public void testRateDecreases() throws InterruptedException {
		this.templateOne.sendBody("test");
		this.templateOne.sendBody("test");
		this.templateOne.sendBody("test");

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
