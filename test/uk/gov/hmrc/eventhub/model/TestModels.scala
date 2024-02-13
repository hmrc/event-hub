/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.eventhub.model

import org.apache.pekko.http.scaladsl.model.{HttpMethods, Uri}
import org.bson.types.ObjectId
import play.api.libs.json.Json
import uk.gov.hmrc.eventhub.config.{Subscriber, TopicName}
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import java.time.{Instant, LocalDateTime}
import java.util.UUID
import scala.concurrent.duration.DurationInt

trait TestModels {

  def toConfigMap(subscriber: Subscriber, topicName: TopicName): Map[String, Any] =
    Map(
      s"${topicName.name}.${subscriber.name}.uri"             -> subscriber.uri.toString(),
      s"${topicName.name}.${subscriber.name}.http-method"     -> subscriber.httpMethod.value,
      s"${topicName.name}.${subscriber.name}.elements"        -> subscriber.elements,
      s"${topicName.name}.${subscriber.name}.per"             -> subscriber.per.toString(),
      s"${topicName.name}.${subscriber.name}.max-connections" -> subscriber.maxConnections,
      s"${topicName.name}.${subscriber.name}.min-back-off"    -> subscriber.minBackOff.toString(),
      s"${topicName.name}.${subscriber.name}.max-back-off"    -> subscriber.maxBackOff.toString(),
      s"${topicName.name}.${subscriber.name}.max-retries"     -> subscriber.maxRetries
    ) ++
      subscriber
        .pathFilter
        .map(filter => Map(s"${topicName.name}.${subscriber.name}.filter-path" -> filter.getPath))
        .getOrElse(Map.empty)

  private val eventJson = Json.parse(
    s"""
       |{
       |  "event": "failed",
       |  "emailAddress": "hmrc-customer@some-domain.org",
       |  "detected": "2021-04-07T09:46:29+00:00",
       |  "code": 605,
       |  "reason": "Not delivering to previously bounced address",
       |  "enrolment": "HMRC-CUS-ORG~EORINumber~1234"
       |}
       |""".stripMargin
  )

  val event: Event = Event(
    eventId = UUID.randomUUID(),
    subject = "foo bar",
    groupId = "in the bar",
    timestamp = LocalDateTime.now(),
    event = eventJson
  )

  def publishedEvent(createdAt: Instant): PublishedEvent =
    PublishedEvent(
      createdAt = createdAt,
      eventId = event.eventId,
      subject = event.subject,
      groupId = event.groupId,
      timestamp = event.timestamp,
      event = event.event
    )

  val now: Instant = Instant.now()
  val workItem: WorkItem[Event] = WorkItem(
    ObjectId.get,
    now,
    now,
    now,
    ProcessingStatus.ToDo,
    0,
    event
  )

  val ChannelPreferencesBounced = "channel-preferences-bounced"
  val ChannelPreferencesBouncedPath = "/channel-preferences/process/bounce"

  val Elements = 100
  val MaxRetries = 5
  val MaxConnections = 4

  val channelPreferences: Subscriber = Subscriber(
    name = ChannelPreferencesBounced,
    uri = Uri(s"http://localhost$ChannelPreferencesBouncedPath"),
    httpMethod = HttpMethods.POST,
    elements = Elements,
    per = 3.seconds,
    maxConnections = MaxConnections,
    minBackOff = 10.millis,
    maxBackOff = 1.second,
    maxRetries = MaxRetries,
    None
  )
}

object TestModels extends TestModels
