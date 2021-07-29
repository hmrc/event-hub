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

package uk.gov.hmrc.eventhub

import akka.http.scaladsl.model.HttpMethods
import play.api.libs.json.Json
import uk.gov.hmrc.eventhub.model.{ Event, Subscriber }
import scala.concurrent.duration._
import java.time.LocalDateTime
import java.util.UUID

class EventHubModuleSpec extends ISpec {

  val event =
    Event(UUID.randomUUID(), "sub", "group", LocalDateTime.MIN, Json.parse("""{"reason":"email not valid"}"""))

  "Configuration" ignore {
    "include topics configuration1" in {
      mongoSetup.topics mustBe Map(
        "notConfigured" -> List(),
        "preferences"   -> List(Subscriber("bounces", "http://localhost:9000/subscriber/email", HttpMethods.POST, 1, 1.second, 10.millis, 100.millis, 0)),
        "email" -> List(
          Subscriber("subscriberName1", "http://localhost:9000/subscriber/email", HttpMethods.POST, 1, 1.second, 10.millis, 100.millis, 0),
          Subscriber("subscriberName2", "http://localhost:9000/subscriber/email", HttpMethods.POST, 1, 1.second, 10.millis, 100.millis, 0)
        )
      )
    }
  }
  "eventRepository" ignore {
    "create event repository" in {
      mongoSetup.eventRepository.collectionName mustBe "event"
    }
  }
  "subscriberRepositories" ignore {
    "create subscriber repositories" in {
      mongoSetup.subscriberRepositories.map(_._2.collectionName) mustBe
        Set("preferences_bounces_queue", "email_subscriberName1_queue", "email_subscriberName2_queue")
    }
  }
  override def externalServices: Seq[String] = Seq.empty[String]
}
