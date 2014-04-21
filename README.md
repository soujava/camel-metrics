#camel-metrics



##Summary
camel-metrics is an [Apache Camel](http://camel.apache.org/) component that uses [Coda Hale Metrics](http://metrics.codahale.com/) to easily expose configurable metrics from your camel route.  The following is the general form for using the component:
```
metrics://metricBaseName?options
```



##Quick Start



#### Setting Up Maven:
```
<dependencies>
    <dependency>
	    <groupId>io.initium.camel</groupId>
	    <artifactId>camel-metrics</artifactId>
	    <version>1.2.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

#### Simple [Spring DSL](http://camel.apache.org/spring.html) Example:
```
<from uri="timer://namedTimer?period=100"/>
<to uri="metrics://yourFirstMetric"/>
```
Depending on your layout, your log have something like this added once per minute.
```
2014-04-17 12:32 - metrics - type=GAUGE, name=yourFirstMetric.sinceHours, value=2.816475E-4
2014-04-17 12:32 - metrics - type=GAUGE, name=yourFirstMetric.sinceMilliseconds, value=1014.437
2014-04-17 12:32 - metrics - type=GAUGE, name=yourFirstMetric.sinceMinutes, value=0.01690885
2014-04-17 12:32 - metrics - type=GAUGE, name=yourFirstMetric.sinceSeconds, value=1.014632
2014-04-17 12:32 - metrics - type=HISTOGRAM, name=yourFirstMetric.intervalHours, count=0, min=0, max=0, mean=0.0, stddev=0.0, median=0.0, p75=0.0, p95=0.0, p98=0.0, p99=0.0, p999=0.0
2014-04-17 12:32 - metrics - type=HISTOGRAM, name=yourFirstMetric.intervalMilliseconds, count=0, min=0, max=0, mean=0.0, stddev=0.0, median=0.0, p75=0.0, p95=0.0, p98=0.0, p99=0.0, p999=0.0
2014-04-17 12:32 - metrics - type=HISTOGRAM, name=yourFirstMetric.intervalMinutes, count=0, min=0, max=0, mean=0.0, stddev=0.0, median=0.0, p75=0.0, p95=0.0, p98=0.0, p99=0.0, p999=0.0
2014-04-17 12:32 - metrics - type=HISTOGRAM, name=yourFirstMetric.intervalSeconds, count=0, min=0, max=0, mean=0.0, stddev=0.0, median=0.0, p75=0.0, p95=0.0, p98=0.0, p99=0.0, p999=0.0
2014-04-17 12:32 - metrics - type=METER, name=yourFirstMetric.rate, count=0, mean_rate=0.0, m1=0.0, m5=0.0, m15=0.0, rate_unit=events/second
```

Note: this documentation is not yet complete.

### Base Metric Group
Typically each metric endpoint creates one group of metrics, this is the base metric group.  It consists of 3 different categories of metrics, 9 metrics in total.  Additionally, you may also add your own custom metrics to the base metric group.

1. **Rate Metric** - The rate metric is a [Meter](http://metrics.codahale.com/manual/core/#meters) metric. Meter.mark() is called once per exchange.  All Meters have 5 attributes: count, mean rate, 1-minute rate, 5-minute rate and 15-minute rate.  See [Meter](http://metrics.codahale.com/manual/core/#meters) for more details.  The default name of the Meter is:
	1. metricName.rate
1. **Since Metrics** - The since metrics are [Gauges](http://metrics.codahale.com/manual/core/#gauges). By default the component creates four gauges, one for each TimeUnit of milliseconds, seconds, minutes and hours.  When each exchange passes through the component the time stamp is recorded.  These gauges show the delta between that timestamp.  All Gauges have 1 attribute: value.  The default names of the Gauges are:
	1. metricName.sinceMilliseconds
	1. metricName.sinceSeconds
	1. metricName.sinceMinutes
	1. metricName.sinceHours
1. **Interval Metrics** - The since metrics are [Histograms](http://metrics.codahale.com/manual/core/#histograms). By default the component creates four histograms, one for each TimeUnit of milliseconds, seconds, minutes and hours.  When each exchange passes through the component the time stamp is recorded.  These histograms are updated with the delta between that timestamp. and the previous exchange's timestamp.  The histograms provide a distribution of the intervals between the exchanges.  Histograms have 11 attributes: count, min, max, mean, stddev, median, 75%, 95%, 98%, 99%, and 99.9%.  See [Histograms](http://metrics.codahale.com/manual/core/#histograms) for more details.  The default names of the Histograms are:
	1. metricName.intervalMilliseconds
	1. metricName.intervalSeconds
	1. metricName.intervalMinutes
	1. metricName.intervalHours
1. **Timing Metric** - The timing metric ia a  [Timer](http://metrics.codahale.com/manual/core/#timers). By default the component does not create a timing metric.  To enable it you must have two metric endpoints in your route.  The first one must have the option: timing=start, the second one must have the option: timing=stop.  The time elapsed between the two endpoints is recorded.  All Timers have 15 attributes: count, mean, 1-minute, 5-minute, 15-minute, min, max, mean, stddev, median, 75%, 95%, 98%, 99%, and 99.9%.  The default name of the timer is:
	1. metricName.timing
1. **Additional Custom Metrics** - There are five types of custom metrics, [Counters](http://metrics.codahale.com/manual/core/#counters), [Meters](http://metrics.codahale.com/manual/core/#meters), [Histograms](http://metrics.codahale.com/manual/core/#histograms), [Gauges](http://metrics.codahale.com/manual/core/#gauges) and [Cached Gauges](http://metrics.codahale.com/manual/core/#cached-gauges).
	1. counters - Counters are defined by two parameters, the name and the value.  Name is optional, if omitted an incrementing name will be chosen.  Value is evaluated at runtime as a [Simple Expression](https://camel.apache.org/simple.html) and used to increment the counter.  The value should evaluate to a Long.  Some examples:
```
<to uri="metrics://yourFirstMetric?counter={value='${simpleExpression}'}"/>
<to uri="metrics://yourFirstMetric?counters=[{name=counterName,value='${simpleExpression}'},{name=otherCounterName,value='${otherSimpleExpression}'}]"/>
```
	1. meters - Meters are defined by two parameters, the name and the value.  Name is optional, if omitted an incrementing name will be chosen.  Value is evaluated at runtime as a [Simple Expression](https://camel.apache.org/simple.html) and used to mark the meter.  The value should evaluate to a Long.  Some examples:
```
<to uri="metrics://yourFirstMetric?meter={value='${simpleExpression}'}"/>
```
```
<to uri="metrics://yourFirstMetric?meters=[{name=meterName,value='${simpleExpression}'},{name=otherMeterName,value='${otherSimpleExpression}'}]"/>
```
	1. histograms - Histograms are defined by two parameters, the name and the value.  Name is optional, if omitted an incrementing name will be chosen.  Value is evaluated at runtime as a [Simple Expression](https://camel.apache.org/simple.html) and used to update the histogram.  The value should evaluate to a Long.
	1. gauges - Gauges are defined by two parameters, the name and the value.  Name is optional, if omitted an incrementing name will be chosen.  Value is evaluated at runtime as a [Simple Expression](https://camel.apache.org/simple.html) and used as the value of the Gauge.  The value need not be a Long.
	1. cachedGauges - Cached Gauges are defined by four parameters, the name, the value, and two parameters that define how frequently to load the value: duration and durationUnit. An example:
	```
	<to uri="metrics://yourFirstMetric?cachedGauge={value='${simpleExpression}',duration=1,durationUnit=minutes}"/>
	```

### Custom Metric Group
If you supply the infix option, it will be evaluated at runtime and a new metric group will be created for each new value.  Note that this new metric group will be used in addition to the base metric group, not instead of.  For example the non-custom metrics from the base group would be named as folows:

	1. metricName.INFIX_VALUE.rate
	1. metricName.INFIX_VALUE.sinceMilliseconds
	1. metricName.INFIX_VALUE.sinceSeconds
	1. metricName.INFIX_VALUE.sinceMinutes
	1. metricName.INFIX_VALUE.sinceHours
	1. metricName.INFIX_VALUE.intervalMilliseconds
	1. metricName.INFIX_VALUE.intervalSeconds
	1. metricName.INFIX_VALUE.intervalMinutes
	1. metricName.INFIX_VALUE.intervalHours


###Reporters
Reporters expose exiting metrics in various ways.  If there are no reporters exposing a metric, there will be no way to see it's value.  Ther are 5 types of supported reporters:
1. Options for All Reporters

| Option | Description | Default | Example Uses |
| --- | --- | --- | --- |
| durationTimeUnit | TimeUnit to use for "duration" metrics  | ... | ... |
| rateTimeUnit | TimeUnit to use for "rate" metrics  | ... | ... |
| filter | regex metric name filter used for base metrics | no default | slf4jReporter={name=myName,filter=^(myMetric01.rate)$} |
| runtimeFilter | regex metric name filter used for custom metric groups  | no default | ... |
| runtimeSimpleFilter | regex metric name filter used for custom metric groups, evaluated as a Simple Expression  | no default | ... |
1. consoleReporters

| Option | Description | Default | Example Uses |
| --- | --- | --- | --- |
| periodDuration | ... | ... | ... |
| periodDurationUnit | ... | ... | ... |

1. slf4jReporters

| Option | Description | Default | Example Uses |
| --- | --- | --- | --- |
| periodDuration | ... | ... | ... |
| periodDurationUnit | ... | ... | ... |
| loggerName | ... | ... | ... |
| runtimeLoggerName | ... | ... | ... |
| runtimeSimpleLoggerName | ... | ... | ... |
| markerName | ... | ... | ... |
| runtimeMarkerName | ... | ... | ... |
| runtimeSimpleMarkerName | ... | ... | ... |

1. consoleReporters

| Option | Description | Default | Example Uses |
| --- | --- | --- | --- |
| periodDuration | ... | ... | ... |
| periodDurationUnit | ... | ... | ... |

1. jmxReporters

| Option | Description | Default | Example Uses |
| --- | --- | --- | --- |
| domain | ... | ... | ... |
| runtimeDomain | ... | ... | ... |
| runtimeSimpleDomain | ... | ... | ... |

1. csvReporters

| Option | Description | Default | Example Uses |
| --- | --- | --- | --- |
| periodDuration | ... | ... | ... |
| periodDurationUnit | ... | ... | ... |
| directory | ... | ... | ... |
| runtimeDirectory | ... | ... | ... |
| runtimeSimpleDirectory | ... | ... | ... |

1. graphiteReporters

| Option | Description | Default | Example Uses |
| --- | --- | --- | --- |
| periodDuration | ... | ... | ... |
| periodDurationUnit | ... | ... | ... |
| host | ... | ... | ... |
| port | ... | ... | ... |
| prefix | ... | ... | ... |
| runtimePrefix | ... | ... | ... |
| runtimeSimplePrefix | ... | ... | ... |

#### Specifying reporters at the component level
You may also configure reporters at the component level.  These reporters will apply to all metric groups.  Any reporters defined at the endpoint level, if named the same, will override the values.  Any reporter defined at the endpoint level for which there is no component level reporter with the same name will be a new reporter.

Example
```
JmxReporterDefinition jmxReporterDefinition = new JmxReporterDefinition();
jmxReporterDefinition.setDomain("testDomain");
MetricsComponent metricsComponent = new MetricsComponent(jmxReporterDefinition);
this.context.addComponent("metrics", metricsComponent);
		this.context.addRoutes(new RouteBuilder() {

			@Override
			public void configure() throws Exception {
        ...
			}
		});
```




###URI Option Reference
| Option | Description | Default | Example Uses |
| --- | --- | --- | --- |
| timing | used to start and stop timing metrics | no default | timing=start, timing=stop |
| infix | evaluated at runtime as a Simple Expression.  Resulting value is used to create additional metrics | no default |  |
| intervalTimeUnits | Used in the creation of interval metrics. | MILLISECONDS, SECONDS, MINUTES, HOURS | intervalTimingUnits=[MILLISECONDS,DAYS] |
| sinceTimeUnits | Used in the creation of since metrics. | MILLISECONDS, SECONDS, MINUTES, HOURS | sinceTimingUnits=[MILLISECONDS,DAYS] |
| timing | used in startting and stopping timers | no default | start, stop |
| timingName |  name of the timer metric | "timing" | any string |
| rateName |  name of the rate metric | "rate" | any string |
| sinceName |  base name of the since metrics | "since" | any string |
| intervalName |  base name of the since metrics | "interval" | any string |
| counters | used to create additional custom counters | no default | counters=[{value='${simpleExpression}'},{value='${otherSimpleExpression}'}] |
| counter | used to create an additional custom counter | no default | counter={value='${simpleExpression}'} |
| meters | used to create additional custom meters | no default | meters=[{value='${simpleExpression}'},{value='${otherSimpleExpression}'}] |
| meter | used to create an additional custom meter | no default | meter={value='${simpleExpression}'} |
| histograms | used to create additional custom histograms | no default | histograms=[{value='${simpleExpression}'},{value='${otherSimpleExpression}'}] |
| histogram | used to create an additional custom histogram | no default | histogram={value='${simpleExpression}'} |
| gauges | used to create additional custom gauges | no default | gauges=[{value='${simpleExpression}'},{value='${otherSimpleExpression}'}] |
| gauge | used to create an additional custom gauge | no default | gauge={value='${simpleExpression}'} |
| cachedGauges | used to create additional custom cached gauges | no default | cachedGauges=[{value='${simpleExpression}'},{value='${otherSimpleExpression}'}] |
| cachedGauge | used to create an additional custom cached gauge | no default | cachedGauge={value='${simpleExpression}'} |
| enableInternalTimer | enable timer of internal processing | false | enableInternalTimer=true |
| consoleReporter | ... | ... | consoleReporters=[{...},{...},...] |
| jmxReporter | ... | ... | jmxReporters=[{...},{...},...] |
| slf4jReporter | ... | ... | slf4jReporters=[{...},{...},...] |
| graphiteReporter | ... | ... | graphiteReporters=[{...},{...},...] |
| csvReporter | ... | ... | csvReporters=[{...},{...},...] |
