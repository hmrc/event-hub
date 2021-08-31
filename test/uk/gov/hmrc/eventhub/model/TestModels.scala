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

import org.bson.types.ObjectId
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

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
}

object TestModels extends TestModels
