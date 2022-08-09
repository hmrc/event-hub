/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json._

import java.time.{Instant, LocalDateTime}
import java.util.UUID

final case class Event(eventId: UUID, subject: String, groupId: String, timestamp: LocalDateTime, event: JsValue)

object Event {
  implicit val eventFormat: OFormat[Event] = Json.format[Event]
}

case class PublishedEvent(
  createdAt: Instant,
  eventId: UUID,
  subject: String,
  groupId: String,
  timestamp: LocalDateTime,
  event: JsValue
)

object PublishedEvent {
  import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.Implicits.jatInstantFormat

  implicit val mongoEventFormat: OFormat[PublishedEvent] = Json.format[PublishedEvent]

  def from(event: Event): PublishedEvent = PublishedEvent(
    createdAt = Instant.now(),
    eventId = event.eventId,
    subject = event.subject,
    groupId = event.groupId,
    timestamp = event.timestamp,
    event = event.event
  )

  def to(mongoEvent: PublishedEvent): Event = Event(
    eventId = mongoEvent.eventId,
    subject = mongoEvent.subject,
    groupId = mongoEvent.groupId,
    timestamp = mongoEvent.timestamp,
    event = mongoEvent.event
  )
}
