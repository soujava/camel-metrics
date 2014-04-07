#camel-metrics

##Summary
camel-metrics is a Apache Camel component that uses [Coda Hale Metrics](http://metrics.codahale.com/) to expose configurable metrics from your camel route via JMX.

##Quick Start
Format:
```
metrics://metricName?options
```
Example Use:
```
<from uri="timer://namedTimer?period=100"/>
<to uri="metrics://yourFirstMetric?context=com.your.company"/>
```
By default you will see three types of default metrics exposed via JMX, "rate", "sinceX", and "intervalX".

##Default Metrics
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
| counterDelta | how much to increment the counter by with each exchange | no default | Integer or Simple Expression |
| counterName |  name of the counter | "count" | any string |
| histogramValue |  the value to be used in the histogram | no default | Integer or Simple Expression |
| histogramName |  name of the histogram | "histogram" | any string |
| gaugeValue |  value to be used in the gauge, note: it doesn't need to be a number.  It can be any object that is usable via JMX | no default | Simple Expression |
| gaugeName |  name of the gauge | "gauge" | any string |
| durationUnit |  time unit to use for all default "duration" metrics | milliseconds | name of any Java TimeUnit value |
| rateUnit | time unit to use for all default "rate" metrics | seconds | name of any Java TimeUnit value |
| gaugeCacheDuration |  duration to be used for the custom cached gauge | 10 | any Integer |
| gaugeCacheDurationUnit |  duration unit to be used for the custom cached gauge | seconds | name of any Java TimeUnit value |
| enableInternalTimer |  enables the internal latency timer | false | true, false |

#What's Next
1. more configurability for metrics already exposed
2. support for more metrics objects from library
3. more configurability of JMX reporting
4. added support for Console reporting
5. added support for Graphite reporting
6. and of course bug fixing
