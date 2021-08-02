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

///*
// * Copyright 2021 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package uk.gov.hmrc.eventhub.repository
//
//import org.mongodb.scala.ClientSession
//import org.scalatestplus.play._
//import org.scalatestplus.play.guice.GuiceOneAppPerSuite
//import play.api.libs.json.Json
//import play.api.test.Helpers.{ await, defaultAwaitTimeout }
//import uk.gov.hmrc.eventhub.model._
//import uk.gov.hmrc.mongo.MongoComponent
//import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
//
//import java.time.LocalDateTime
//import java.util.UUID
//import javax.inject.Inject
//import scala.concurrent.ExecutionContext.Implicits.global
//
//class EventRepositorySpec @Inject() extends PlaySpec with GuiceOneAppPerSuite {
//
//  "addEvent" must {
//    "return true" in new TestCase {
//      await(eventRepository.addEvent(clientSession, repository, event).toFuture().map(_.wasAcknowledged())) mustBe true
//    }
//  }
//
//  "saveEvent" must {
//    "return Event if exists" in new TestCase {
//      await(eventRepository.addEvent(clientSession, repository, event).toFuture())
//      await(eventRepository.find(eventId, repository)).head.eventId mustBe UUID.fromString(eventId)
//    }
//  }
//
//  class TestCase {
//    val mongo = app.injector.instanceOf[MongoComponent]
//    val eventId = UUID.randomUUID().toString
//    val repository = new PlayMongoRepository[Event](
//      mongoComponent = mongo,
//      "event",
//      Event.eventFormat,
//      indexes = Seq(),
//    )
//
//    val eventRepository = new EventRepository()
//
//    val event =
//      Event(UUID.fromString(eventId), "sub", "group", LocalDateTime.MIN, Json.parse("""{"reason":"email not valid"}"""))
//
//    val clientSession: ClientSession = await(mongo.client.startSession().head())
//  }
//}
