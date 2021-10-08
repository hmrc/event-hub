#### License
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

## event-hub

A messaging middleware solution to decouple sending and receiving of events between distributed applications. The event hub offers fan out of messages to subscribers over HTTP.

## Configuration

### Convention

You can pass system properties to configure topics and subscribers in the relevant environment specific yaml files. The configuration keys have the following namespace convention:
   * `[topic name].[subscriber name].[property]`

Where:
   * `[topic name]`
     - Description: The name of the topic to be configured
     - Type: String
     - Format: `[A-Z-a-z\-]`
     - Example: `email`
        

   * `[subscriber name]`
     - Description: The name of the subscriber
     - Type: String
     - Format: `[A-Z-a-z\-]`
     - Example: `customer-data-store`
   

   * `[property]`
     - Description: A set of possible properties for each subscriber
     - Range: `∈ {uri, http-method, elements, per, max-connections, min-back-off, max-back-off, max-retries, filter-path}`
     - Example: `uri`
     

### Subscriber properties

All subscriber properties apart from `uri` have a default value. To successfully configure a subscriber providing the `uri` property alone is sufficient.

* uri
    - Description: the URI of the subscriber endpoint
    - Type: URI  
    - Example: `http://localhostt:8080/appname/path`


* http-method
    - Description: The HTTP method to use for the subscriber endpoint 
    - Type: String
    - Range: `∈ {POST, PUT}`
    - Default: `POST`
    - Example: `POST`


* elements
    - Description: The number of elements to emit down stream. Used in conjunction with `per` to throttle requests.
    - Type: Int
    - Default: `60`
    - Example: `10`


* per
    - Description: A time over which to send a number of requests, `elements`, to the subscriber.
    - Type: Finite duration
    - Default: `3.seconds`
    - Example: `1.minute`


* max-connections
    - Description: Controls the maximum number of pooled HTTP connections (max degree of parallelism) that can be open at any one time to a subscriber.
    - Type: Int
    - Default: `4`  
    - Example: `16`


* min-back-off
    - Description: The minimum back off time between request attempts when retrying. Used in conjunction with `max-back-off` & `max-retries` to calculate an exponential back off and retry algorithm for handling retryable requests.
    - Type: Finite duration
    - Default: `100.millis`
    - Example: `1.minute`


* max-back-off
    - Description: The maximum back off time between request attempts when retrying.
    - Type: Finite duration
    - Default: `10.minutes`
    - Example: `2.minutes`


* max-retries
    - Description: The maximum amount of times to retry a request.
    - Type: Int
    - Default: `150`
    - Example: `10`


* filter-path
    - Description: An optional JSONPath expression, if present only requests that match the subscriber topic and this filter will be published to the subscriber. 
    - Type: [JSONPath](https://goessner.net/articles/JsonPath/)
    - Default: None  
    - Example: `"$.event[?(@.enrolment =~ /HMRC\\-CUS\\-ORG\\~EORINumber~.*/i)]"`


### A complete example

```
    topics.email.channel-preferences.uri: https://channel-preferences.protected.mdtp:443/channel-preferences/process/bounce
    topics.email.channel-preferences.http-method: POST
    topics.email.channel-preferences.elements: 100
    topics.email.channel-preferences.per: 3.seconds
    topics.email.channel-preferences.max-connections: 4
    topics.email.channel-preferences.min-back-off: 5.millis
    topics.email.channel-preferences.mix-back-off: 10.millis
    topics.email.channel-preferences.max-retries: 5
    topics.email.channel-preferences.filter-path: $.event[?(@.enrolment =~ /HMRC\\-CUS\\-ORG\\~EORINumber~.*/i)]
```

## Back off and retry

We have employed Akka's back off and retry algorithm, [see](https://github.com/akka/akka/blob/master/akka-actor/src/main/scala/akka/pattern/BackoffSupervisor.scala#L312). 
This algorithm has an exponent of `2` and so the time between retries will double each time until `max-back-off` is reached. We have a fixed random factor of `0.2` and so will always add jitter to the calculation. 
The default configuration values provide back-off and retry over a ~ 23-hour period, after which the request will be marked as failed in the work item repository and will be tried again provided the configuration of the repository allows.


### Work item repository configuration - [HOW_TO_USE](https://github.com/hmrc/work-item-repo/blob/master/HOW_TO_USE.md)

Each subscriber has its own work item repository for which the events to publish are stored. 
The streaming subscriber pipeline polls this repository when there is an available http connection. 
Events that are retryable will be marked a failed once they have been retried for the last time in accordance with the configured back off and retry algorithm.
The configuration values that determine if a failed work item should be picked up again when polled are global and therefore apply to all subscribers, here are the configuration options with their defaults:

```
    queue.retryAfter = 24 hours   // the time after which an in progress work item can be selected again.
    queue.numberOfRetries = 3  // the maximum number of times a work item can be selected again (retried) before being marked as permanently failed.
    queue.retryFailedAfter = 1 hour // the time after which a work item has been marked as failed that it can be retried.
    queue.deleteEventAfter = 7 days // To be implemented
```



## Starting locally
 * Depending on the subscribers you have configured you will need to assess which services you need to have running. Starting `DC_ALL` in service manager, `sm --start DC_ALL` works for the channel preferences example in this readme.
 * Mongo needs to be running with a replica set, you can start an instance by running `docker-compose up` from the project root.
 * You can choose to startup locally with configuration overrides from a config file, some are provided already in `dev.conf` which will provision a default topic and subscriber:
   - `sbt "start -Dconfig.resource=dev.conf"`
 * Alternatively you can pass system properties at startup like so: 
   - `sbt "start -Dtopics.email.channel-preferences-bounced-emails.uri=http://localhost:9052/channel-preferences/process/bounce"`

### An example request that targets the subscriber configured in `dev.conf`

```
curl -v -X POST -H "Content-Type: application/json" http://localhost:9050/event-hub/publish/email -d '
{
    "eventId": "623b6f96-d36f-4014-8874-7f3f8287f9e6", 
    "subject": "calling", 
    "groupId": "su users",
    "timestamp": "2021-07-01T13:09:29Z",
    "event" : {
        "event": "failed",
        "emailAddress": "hmrc-customer@some-domain.org",
        "detected": "2021-04-07T09:46:29+00:00",
        "code": 605,
        "reason": "Not delivering to previously bounced address",
        "enrolment": "HMRC-CUS-ORG~EORINumber~GB123456789"
    }
}'
```

### Adding local configuration for topics
* this configuration goes inside topics in config
* from below email is name of a topic
* channel-preferences and customs-data-store  object from below is example of subscriber configuration, we have to define this for each subscriber
* topic and subscriber name (channel-preferences from below) are important because a mongo collection will be named using this
* collection name will be in format topic_subscriberName_queue, from below example collection name will be email_channel-preferences_queue
* if payload that's passed from publish endpoint does not match filterPath for a subscriber, this event will be ignored for that subscriber's queue
* we can use this page https://jsonpath.com/ or http://jsonpath.herokuapp.com/ to validate payload for filterPath
* make sure we don't define duplicate topics and subscribers this can yield incorrect results 

```topics {
  email {
    channel-preferences {
      uri = "http://localhost:9052/channel-preferences/process/bounce"
      http-method = "POST"
      elements = 100
      per = 3.seconds
      max-connections = 4
      min-back-off = 5.millis
      max-back-off = 10.millis
      max-retries = 5
      filterPath = "$.event[?(@.enrolment =~ /HMRC\\-CUS\\-ORG\\~EORINumber~.*/i)]"
    },
    customs-data-store {
      uri = "http://localhost:9052/channel-preferences/process/bounce"
      http-method = "POST"
      elements = 100
      per = 3.seconds
      max-connections = 4
      min-back-off = 5.millis
      max-back-off = 10.millis
      max-retries = 5
      filterPath = "$.event[?(@.enrolment =~ /HMRC\\-CUS\\-ORG\\~EORINumber~.*/i)]"
    }
  }
}
```

### Example for adding configuration for topics in `app-config-$env` files
#### below example shows configuration for topic `email` that has 2 subscribers `channel-preferences` and `customs-data-store`

```
    topics.email.channel-preferences.uri: https://channel-preferences.protected.mdtp:443/channel-preferences/process/bounce
    topics.email.channel-preferences.http-method: POST
    topics.email.channel-preferences.elements: 100
    topics.email.channel-preferences.per: 3.seconds
    topics.email.channel-preferences.max-connections: 4
    topics.email.channel-preferences.min-back-off: 5.millis
    topics.email.channel-preferences.mix-back-off: 10.millis
    topics.email.channel-preferences.max-retries: 5
    topics.email.channel-preferences.filter-path: $.event[?(@.enrolment =~ /HMRC\\-CUS\\-ORG\\~EORINumber~.*/i)]

    topics.email.customs-data-store.uri: https://customs-data-store.protected.mdtp:443/channel-preferences/process/bounce
    topics.email.customs-data-store.http-method: POST
    topics.email.customs-data-store.elements: 100
    topics.email.customs-data-store.per: 3.seconds
    topics.email.customs-data-store.max-connections: 4
    topics.email.customs-data-store.min-back-off: 5.millis
    topics.email.customs-data-store.mix-back-off: 10.millis
    topics.email.customs-data-store.max-retries: 5
    topics.email.customs-data-store.filter-path: $.event[?(@.enrolment =~ /HMRC\\-CUS\\-ORG\\~EORINumber~.*/i)]
```

### Adding local configuration for event repository

The following allows you to configure a time-to-live expiry in seconds on individual events stored in the dedup events collection

```
event-repo {
  expire-after-seconds-ttl = 86400
}
```

### Adding local configuration for subscriber repositories

The following allows you to configure a time-to-live expiry in seconds on individual events stored in the subscriber collections

```
subscriber-repos {
  expire-after-seconds-ttl = 86400
}
```

## Stats

Custom stats for event-hub can be collected and viewed locally by setting up graphite and grafana importing the even-hub dashboard json from [`grafana-dashboard/target/output/grafana`]("https://github.com/hmrc/grafana-dashboards#grafana-dashboards") into grafana and inserting some events.
* To set up graphite and grafana from project root run:
    ```
        docker volume create --name=grafana-volume
        docker-compose -f stats.yml up 
    ```
* When logging into grafana on `http://localhost:3000` the default username and password is `admin`
* Configure a graphite datasource to `http://graphite:8080`
* Running integration tests, in particular `uk.gov.hmrc.eventhub.subscription.SubscriberPushSubscriptionsISpec`, is a convenient way to generate most metrics.

## Run the tests and sbt fmt before raising a PR

Format:

`sbt fmt`

Then run the tests and coverage report:

`sbt clean coverage test coverageReport`

If your build fails due to poor test coverage, *DO NOT* lower the test coverage threshold, instead inspect the generated report located here on your local repo: `/target/scala-2.12/scoverage-report/index.html`

Then run the integration tests:

`sbt it:test`
