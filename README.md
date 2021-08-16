#### License
This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

## event-hub

Facilitate reporting of delivery of failed email's

### Starting locally
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
    "timeStamp": "2021-07-01T13:09:29Z",
    "event" : {
        "event": "failed",
        "emailAddress": "hmrc-customer@some-domain.org",
        "detected": "2021-04-07T09:46:29+00:00",
        "code": 605,
        "reason": "Not delivering to previously bounced address",
        "enrolment": "HMRC-MTD-VAT~VRN~GB123456789"
    }
}'
```
### Adding configuration for topics

* topics will have comma separated list of topic's
* each topic can have comma separated list of subscriber configuration objects
* from below email is name of a topic
* bounced-emails object from below is example of subscriber configuration, we have to define this for each subscriber
* topic and subscriber name (bounced-email from below) are important because a mongo collection will be named using this
* collection name will be in format topic_subscriberName_queue, from below example collection name will be email_bounced-emails_queue
* if payload that's passed from publish endpoint dint match filterPath, this subscriber will be ignored and will not be added to queue
* we can use this page https://jsonpath.com/ to validate payload against filterPath

```topics {
  email {
    bounced-emails {
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
}```
