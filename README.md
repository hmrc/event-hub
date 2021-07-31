
# event-hub

Facilitate reporting of delivery of failed email's

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

```
curl -v -X POST -H "Content-Type: application/json" http://localhost:9050/event-hub/publish/bounced-emails -d '
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

```
curl -v -X POST -H "Content-Type: application/json" http://localhost:9050/event-hub/publish/no-topic -d '
{
    "eventId": "b1b12166-e7b1-4769-9cf6-4baf3c468af8", 
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

```
curl -v -X POST -H "Content-Type: application/json" https://event-hub.protected.mdtp/event-hub/publish/bounced-emails -d '
{
    "eventId": "b5374fb2-e741-492e-aa49-be076f774500", 
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