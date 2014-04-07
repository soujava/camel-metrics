package io.initium.camel.component.metrics;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Ignore;
import org.junit.Test;

public class MetricsTest extends CamelTestSupport {

	private static Random	random	= new Random();

	@Override
	public boolean isUseRouteBuilder() {
		return false;
	}

	@Test
	@Ignore
	public void theFirstTest() throws Exception {
		this.context.addRoutes(new RouteBuilder() {
			@Override
			public void configure() throws Exception {

				Processor myRandomProcessor = new Processor() {
					@Override
					public void process(final Exchange exchange) throws Exception {
						StringBuilder stringBuilder = new StringBuilder();
						int size = random.nextInt(10);
						for (int i = 1; i <= size; i++) {
							stringBuilder.append(size);
						}
						exchange.getIn().setBody(stringBuilder.toString());
					}
				};
				from("timer://namedTimer?period=100").to("metrics://sampleWithoutTiming?context=context01");
				from("timer://namedTimer?period=200").to("metrics://sampleWithoutTiming?context=context02").delay(1000);
				from("timer://namedTimer?period=100").to("metrics://sampleWithTiming?context=context02&timing=start").delay(1000).to("metrics://sampleWithTiming?timing=stop");
				// from("timer://namedTimer?period=100").to("metrics://sampleWithCounter1?context=context03&counterDelta=3&counterName=MyName");
				// from("timer://namedTimer?period=100").setHeader("testCounterDelta",
				// simple("2")).to("metrics://sampleWithCounter2?counterDelta=${in.header.testCounterDelta}");
				// from("timer://namedTimer?period=100").to("metrics://sampleWithHistogram1?histogramValue=3");
				// from("timer://namedTimer?period=100").process(myRandomProcessor).to("metrics://sampleWithHistogram2?histogramValue=${in.body.length()}");
				// from("timer://namedTimer?period=100").to("metrics://sampleWithGauge1?gaugeValue=sdsdds");
				// from("timer://namedTimer?period=100").process(myRandomProcessor).to("metrics://sampleWithGauge2?gaugeValue=${in.body}");
			}
		});
		this.context.start();
		while (true) {
			TimeUnit.MINUTES.sleep(1);
		}
	}

	@Test
	@Ignore
	public void theSecondTest() throws Exception {
		this.context.addRoutes(new RouteBuilder() {
			@Override
			public void configure() throws Exception {
				from("timer://namedTimer?period=100").to("metrics://sample?context=context1&enableInternalTimer=true");
				from("timer://namedTimer?period=100").to("metrics://sample?context=context2&durationUnit=seConds");
				from("timer://namedTimer?period=100").to("metrics://sample?context=context3&histogramValue=1&histogramReservoir=slidingTimewindow");
				from("timer://namedTimer?period=100").to("metrics://sample?context=context4&histogramValue=1&histogramReservoir=slidingTimewindow&slidingTimeWindowDuration=10&slidingTimeWindowDurationUnit=seconds");
			}
		});
		this.context.start();
		while (true) {
			TimeUnit.MINUTES.sleep(1);
		}

	}

}
