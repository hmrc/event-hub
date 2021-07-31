/*
 * Copyright 2021 HM Revenue & Customs
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

import akka.http.scaladsl.model.{HttpMethods, Uri}
import org.bson.types.ObjectId
import play.api.libs.json.Json
import uk.gov.hmrc.eventhub.config.Subscriber
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import scala.concurrent.duration._
import java.time.{Instant, LocalDateTime}
import java.util.UUID

trait TestModels {
  private val eventJson = Json.parse(
    s"""
       |{
       |  "event": "failed",
       |  "emailAddress": "hmrc-customer@some-domain.org",
       |  "detected": "2021-04-07T09:46:29+00:00",
       |  "code": 605,
       |  "reason": "Not delivering to previously bounced address",
       |  "enrolment": "HMRC-MTD-VAT~VRN~GB123456789"
       |}
       |""".stripMargin
  )

  val event: Event = Event(
    eventId = UUID.randomUUID(),
    subject = "foo bar",
    groupId = "in the bar",
    timeStamp = LocalDateTime.now(),
    event = eventJson
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

  val MaxConnections = 4

  val subscriber: Subscriber = Subscriber(
    name = "foo subscriber",
    uri = Uri("http://localhost:8080/foo"),
    httpMethod = HttpMethods.POST,
    elements = 0,
    per = 1.second,
    maxConnections = MaxConnections,
    minBackOff = 1.second,
    maxBackOff = 2.seconds,
    maxRetries = 2
  )

  val idempotentSubscriber: Subscriber = subscriber.copy(
    httpMethod = HttpMethods.PUT,
    uri = Uri("http://localhost:8081/foo")
  )
}

object TestModels extends TestModels
