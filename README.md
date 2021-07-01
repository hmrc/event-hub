
# event-hub

Facilitate reporting of delivery of failed email's

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").


curl \
-X POST \
-H "Content-Type: application/json" \
http://localhost:9051/event-hub/publish/email \
-d '
{
    "eventId": "623b6f96-d36f-4014-8874-7f3f8287f9e6", 
    "subject": "calling", 
    "groupId": "su users",
    "timeStamp": "2021-07-01T13:09:29Z",
    "event": { "name": "some-event" }
}'