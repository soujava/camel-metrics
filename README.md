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


### Standard Metric Groups
Typically each metric endpoint creates one group of metrics, this is the standard metric group.  It consists of 3 different categories of metrics, 9 metrics in total.

1. Rate Metric - The rate metric is a [Meter](http://metrics.codahale.com/manual/core/#meters) metric. Meter.mark() is called once per exchange.  All Meters have 5 attributes: count, mean rate, 1-minute rate, 5-minute rate and 15-minute rate.  See [Meter](http://metrics.codahale.com/manual/core/#meters) for more details.  The default name of the Meter is:
	1. metricName.rate
1. Since Metrics - The since metrics are [Gauges](http://metrics.codahale.com/manual/core/#gauges). By default the component creates four gauges, one for each TimeUnit of milliseconds, seconds, minutes and hours.  When each exchange passes through the component the time stamp is recorded.  These gauges show the delta between that timestamp.  All Gauges have 1 attribute: value.  The default names of the Gauges are:
	1. metricName.sinceMilliseconds
	1. metricName.sinceSeconds
	1. metricName.sinceMinutes
	1. metricName.sinceHours
1. Interval Metrics - The since metrics are [Histograms](http://metrics.codahale.com/manual/core/#histograms). By default the component creates four histograms, one for each TimeUnit of milliseconds, seconds, minutes and hours.  When each exchange passes through the component the time stamp is recorded.  These histograms are updated with the delta between that timestamp. and the previous exchange's timestamp.  The histograms provide a distribution of the intervals between the exchanges.  Histograms have 11 attributes: count, min, max, mean, stddev, median, 75%, 95%, 98%, 99%, and 99.9%.  See [Histograms](http://metrics.codahale.com/manual/core/#histograms) for more details.  The default names of the Histograms are:
	1. metricName.intervalMilliseconds
	1. metricName.intervalSeconds
	1. metricName.intervalMinutes
	1. metricName.intervalHours
1. Timing Metric - The timing metric ia a  [Timer](http://metrics.codahale.com/manual/core/#timers). By default the component does not create a timing metric.  To enable it you must have two metric endpoints in your route.  The first one must have the option: timing=start, the second one must have the option: timing=stop.  The time elapsed between the two endpoints is recorded.  All Timers have 15 attributes: count, mean, 1-minute, 5-minute, 15-minute, min, max, mean, stddev, median, 75%, 95%, 98%, 99%, and 99.9%.  The default name of the timer is:
	1. metricName.timing
1. Custom Metrics - Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam urna tortor, venenatis ut suscipit lacinia, auctor in metus. Etiam semper vehicula nisi, et feugiat quam scelerisque hendrerit. Sed ullamcorper nisi vitae lobortis interdum.
	1. counters - Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam urna tortor, venenatis ut suscipit lacinia, auctor in metus. Etiam semper vehicula nisi, et feugiat quam scelerisque hendrerit. Sed ullamcorper nisi vitae lobortis interdum.
	1. histograms - Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam urna tortor, venenatis ut suscipit lacinia, auctor in metus. Etiam semper vehicula nisi, et feugiat quam scelerisque hendrerit. Sed ullamcorper nisi vitae lobortis interdum.
	1. gauges - Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam urna tortor, venenatis ut suscipit lacinia, auctor in metus. Etiam semper vehicula nisi, et feugiat quam scelerisque hendrerit. Sed ullamcorper nisi vitae lobortis interdum.
	1. cachedGauges - Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam urna tortor, venenatis ut suscipit lacinia, auctor in metus. Etiam semper vehicula nisi, et feugiat quam scelerisque hendrerit. Sed ullamcorper nisi vitae lobortis interdum


###Reporters
...

###Default Metrics
"rate" in camel-metrics is a Meter in Coda Hale Metrics, see http://metrics.codahale.com/getting-started/#meters for more details on Meters.  Every exchange will cause a call to Meter.mark().

"sinceX" in camel-metrics are Gauges in Coda Hale Metrics, see http://metrics.codahale.com/getting-started/#gauges more details for on Gauges. They come in 7 different varieties corresponding to the 7 values of java's TimeUnit enumeration: ,sinceNanoseconds, sinceMicroseconds, sinceMilliseconds, sinceSeconds, sinceMinutes, sinceHours and sinceDays.  The value of the metrics corresponds to how long it's been since an exchange has been processed this endpoint.  In the case of sinceSeconds, it's the number of seconds since an exchange has been processed by this endpoint, in the case of sinceMinutes, it's the number of seconds since an exchange has been processed by this endpoint, and so on.

"intervalX" in camel-metrics are Histograms in Coda Hale Metrics, see http://metrics.codahale.com/getting-started/#histograms for more details on Histograms. They also come in the 7 different TimeUnit varieties as the "sinceX" metrics.  The difference between "intervalX" and "sinceX" metrics is that "sinceX" metrics provide a real-time view of the time interval between exchanges, whereas "intervalX" metrics provide a view of the distribution of the time interval between exchanges.

##Timing Metrics
You can also create a timing metric.  This is a basic example:
```
<from uri="timer://namedTimer?period=100"/>
<to uri="metrics://yourSecondMetric?context=com.your.company&timing=start"/>
<!-- ... more work here that you want to time ... -->
<to uri="metrics://yourFirstMetric?context=com.your.company&timing=stop"/>
```
Timing metrics in camel-metrics are Timers in Coda Hale Metrics, see http://metrics.codahale.com/getting-started/#timers for more details.  The first occurance of the named metrics endpoint will start the timer, the second one will stop it.

##Additional Custom Metrics
You can also enable some metrics based on [Apache Camel's Simple Expression Language](https://camel.apache.org/simple.html).  Here are some simple examples:
###Counter
```
<from uri="timer://namedTimer?period=100"/>
<to uri="metrics://myThirdMetric?context=com.your.company&counterDelta=3&counterName=MyName"/>
```
The value of `counterDelta` can be any Simple Expression.
###Histogram
```
<from uri="timer://namedTimer?period=100"/>
<to uri="metrics://myFourthMetric?histogramValue=3&histogramName=MyName"/>
```
The value of `histogramValue` can be any Simple Expression.
###Gauge
```
<from uri="timer://namedTimer?period=100"/>
<to uri="metrics://myFourthMetric?gaugeValue=3&gaugeName=MyName"/>
```
The value of `gaugeValue` can be any Simple Expression.  Note that since a custom Gauge metric could be time consuming, the Gauge that is created is a Cached Gauge, see http://metrics.codahale.com/manual/core/#cached-gauges for more details on Cached Gauges.

###Internal Timer
Every component added to a route adds latency, we are making every effort to minimize the latency added by camel-metrics.  The `enableInternalTimer` flag cane be enabled to expose how much latency each camel-metrics endpoint is adding.

###URI Option Reference
| Option | Description | Default | Allowed Values |
| --- | --- | --- | --- |
| context | prefix to use before all the individual metrics | "io.initium.metrics" | any string |
| timing | used in startting and stopping timers | no default | start, stop |
| timingName |  name of the timer | "timing" | any string |
| timingReservoir |  name of supplied reservoir to use for the timing  |  | |
| counterDelta | how much to increment the counter by with each exchange | no default | Integer or Simple Expression |
| counterName |  name of the counter | "count" | any string |
| histogramValue |  the value to be used in the histogram | no default | Integer or Simple Expression |
| histogramName |  name of the histogram | "histogram" | any string |
| histogramReservoir |  name of supplied reservoir to use for the custom histogram  |  | |
| gaugeValue |  value to be used in the gauge, note: it doesn't need to be a number.  It can be any object that is usable via JMX | no default | Simple Expression |
| gaugeName |  name of the gauge | "gauge" | any string |
| durationUnit |  time unit to use for all default "duration" metrics | milliseconds | name of any Java TimeUnit value |
| rateUnit | time unit to use for all default "rate" metrics | seconds | name of any Java TimeUnit value |
| gaugeCacheDuration |  duration to be used for the custom cached gauge | 10 | any Integer |
| gaugeCacheDurationUnit |  duration unit to be used for the custom cached gauge | seconds | name of any Java TimeUnit value |
| intervalReservoir |  name of supplied reservoir to use for the interval  |  | |
| enableInternalTimer |  enables the internal latency timer | false | true, false |
| enableJmxReporting |  enables the JMX Reporting | true | true, false |

#What's Next
1. more configurability for metrics already exposed
2. support for more metrics objects from library
3. more configurability of JMX reporting
4. added support for Console reporting
5. added support for Graphite reporting
6. and of course bug fixing
