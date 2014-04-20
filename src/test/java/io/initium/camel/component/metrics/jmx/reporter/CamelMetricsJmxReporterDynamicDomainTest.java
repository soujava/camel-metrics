package io.initium.camel.component.metrics.jmx.reporter;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;

public class CamelMetricsJmxReporterDynamicDomainTest extends CamelMetricsJmxReporterTestSupport {

	@EndpointInject(uri = "mock:resultOne")
	protected MockEndpoint		resultEndpointOne;

	@Produce(uri = "direct:startOne")
	protected ProducerTemplate	templateOne;

	@EndpointInject(uri = "mock:resultTwo")
	protected MockEndpoint		resultEndpointTwo;

	@Produce(uri = "direct:startTwo")
	protected ProducerTemplate	templateTwo;

	@Test
	public void testDynamicDomainAndInfixJmx() {
		Map<String, Object> headers1 = new HashMap<String, Object>();
		Map<String, Object> headers2 = new HashMap<String, Object>();
		Map<String, Object> headers3 = new HashMap<String, Object>();

		headers1.put("infix", "infix1");
		headers1.put("domain", "dom1");

		headers2.put("infix", "infix2");
		headers2.put("domain", "dom2");

		headers3.put("infix", "infix3");
		headers3.put("domain", "dom3");

		this.templateOne.sendBodyAndHeaders("test", headers1);
		this.templateOne.sendBodyAndHeaders("test", headers2);
		this.templateOne.sendBodyAndHeaders("test", headers3);

		// Basic metrics
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.interval.hours"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.interval.minutes"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.interval.seconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.interval.milliseconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.since.hours"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.since.minutes"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.since.seconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.since.milliseconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test.rate"), equalTo(true));
		assertThat(verifyAttributeValueLong("metrics:name=test.rate", "Count", 3L), equalTo(true));

		// dom1 metrics
		assertThat(verifyObjectNameIsRegistered("dom1:name=test.infix1.interval.hours"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom1:name=test.infix1.interval.minutes"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom1:name=test.infix1.interval.seconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom1:name=test.infix1.interval.milliseconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom1:name=test.infix1.since.hours"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom1:name=test.infix1.since.minutes"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom1:name=test.infix1.since.seconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom1:name=test.infix1.since.milliseconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom1:name=test.infix1.rate"), equalTo(true));
		assertThat(verifyAttributeValueLong("dom1:name=test.infix1.rate", "Count", 1L), equalTo(true));

		// dom2 metrics
		assertThat(verifyObjectNameIsRegistered("dom2:name=test.infix2.interval.hours"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom2:name=test.infix2.interval.minutes"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom2:name=test.infix2.interval.seconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom2:name=test.infix2.interval.milliseconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom2:name=test.infix2.since.hours"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom2:name=test.infix2.since.minutes"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom2:name=test.infix2.since.seconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom2:name=test.infix2.since.milliseconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom2:name=test.infix2.rate"), equalTo(true));
		assertThat(verifyAttributeValueLong("dom2:name=test.infix2.rate", "Count", 1L), equalTo(true));

		// dom3 metrics
		assertThat(verifyObjectNameIsRegistered("dom3:name=test.infix3.interval.hours"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom3:name=test.infix3.interval.minutes"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom3:name=test.infix3.interval.seconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom3:name=test.infix3.interval.milliseconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom3:name=test.infix3.since.hours"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom3:name=test.infix3.since.minutes"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom3:name=test.infix3.since.seconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom3:name=test.infix3.since.milliseconds"), equalTo(true));
		assertThat(verifyObjectNameIsRegistered("dom3:name=test.infix3.rate"), equalTo(true));
		assertThat(verifyAttributeValueLong("dom3:name=test.infix3.rate", "Count", 1L), equalTo(true));

		this.resultEndpointOne.expectedMessageCount(3);

	}

	@Test
	public void testDynamicFilterJmx() {
		Map<String, Object> headers1 = new HashMap<String, Object>();
		Map<String, Object> headers2 = new HashMap<String, Object>();
		Map<String, Object> headers3 = new HashMap<String, Object>();

		headers1.put("infix", "infixfilter1");
		headers1.put("domain", "domfilter1");

		headers2.put("infix", "infixfilter2");
		headers2.put("domain", "domfilter2");

		headers3.put("infix", "infixfilter3");
		headers3.put("domain", "domfilter3");

		this.templateTwo.sendBodyAndHeaders("test", headers1);
		this.templateTwo.sendBodyAndHeaders("test", headers2);
		this.templateTwo.sendBodyAndHeaders("test", headers3);

		// Basic metrics
		assertThat(verifyObjectNameIsRegistered("metrics:name=test2.intervalHours"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test2.intervalMinutes"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test2.intervalSeconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test2.intervalMilliseconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test2.sinceHours"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test2.sinceMinutes"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test2.sinceSeconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test2.sinceMilliseconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("metrics:name=test2.rate"), equalTo(true));
		assertThat(verifyAttributeValueLong("metrics:name=test2.rate", "Count", 3L), equalTo(true));

		// dom1 metrics
		assertThat(verifyObjectNameIsRegistered("domfilter1:name=test2.infixfilter1.intervalHours"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter1:name=test2.infixfilter1.intervalMinutes"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter1:name=test2.infixfilter1.intervalSeconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter1:name=test2.infixfilter1.intervalMilliseconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter1:name=test2.infixfilter1.sinceHours"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter1:name=test2.infixfilter1.sinceMinutes"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter1:name=test2.infixfilter1.sinceSeconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter1:name=test2.infixfilter1.sinceMilliseconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter1:name=test2.infixfilter1.rate"), equalTo(true));
		assertThat(verifyAttributeValueLong("domfilter1:name=test2.infixfilter1.rate", "Count", 1L), equalTo(true));

		// dom2 metrics
		assertThat(verifyObjectNameIsRegistered("domfilter2:name=test2.infixfilter2.intervalHours"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter2:name=test2.infixfilter2.intervalMinutes"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter2:name=test2.infixfilter2.intervalSeconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter2:name=test2.infixfilter2.intervalMilliseconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter2:name=test2.infixfilter2.sinceHours"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter2:name=test2.infixfilter2.sinceMinutes"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter2:name=test2.infixfilter2.sinceSeconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter2:name=test2.infixfilter2.sinceMilliseconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter2:name=test2.infixfilter2.rate"), equalTo(true));
		assertThat(verifyAttributeValueLong("domfilter2:name=test2.infixfilter2.rate", "Count", 1L), equalTo(true));

		// dom3 metrics
		assertThat(verifyObjectNameIsRegistered("domfilter3:name=test2.infixfilter3.intervalHours"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter3:name=test2.infixfilter3.intervalMinutes"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter3:name=test2.infixfilter3.intervalSeconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter3:name=test2.infixfilter3.intervalMilliseconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter3:name=test2.infixfilter3.sinceHours"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter3:name=test2.infixfilter3.sinceMinutes"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter3:name=test2.infixfilter3.sinceSeconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter3:name=test2.infixfilter3.sinceMilliseconds"), equalTo(false));
		assertThat(verifyObjectNameIsRegistered("domfilter3:name=test2.infixfilter3.rate"), equalTo(true));
		assertThat(verifyAttributeValueLong("domfilter3:name=test2.infixfilter3.rate", "Count", 1L), equalTo(true));

		this.resultEndpointOne.expectedMessageCount(3);

	}

	@Override
	protected RouteBuilder createRouteBuilder() {
		return new RouteBuilder() {
			@Override
			public void configure() {
				// from("direct:startOne").to("log:io?showAll=true").to("metrics://test?jmxReporters=[{dynamicDomain='${header.domain}'}]").to("mock:resultOne");
				from("direct:startOne").to("metrics://test?infix=${header.infix}&jmxReporters=[{runtimeSimpleDomain='${header.domain}'}]").to("mock:resultOne");
				from("direct:startTwo").to("metrics://test2?infix=${header.infix}&jmxReporters=[{runtimeSimpleDomain='${header.domain}',filter=^(.*.rate)$,runtimeSimpleFilter=^(.*.rate)$}]").to("mock:resultTwo");
			};
		};
	}
}
