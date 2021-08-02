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
//import org.mockito.MockitoSugar
//import org.mongodb.scala.ClientSession
//import org.mongodb.scala.bson.ObjectId
//import org.scalatestplus.play.PlaySpec
//import org.scalatestplus.play.guice.GuiceOneAppPerSuite
//import play.api.libs.json.Json
//import play.api.test.Helpers.{ await, defaultAwaitTimeout }
//import uk.gov.hmrc.eventhub.model.Event
//import uk.gov.hmrc.mongo.MongoComponent
//import uk.gov.hmrc.mongo.workitem.{ ProcessingStatus, WorkItem, WorkItemFields, WorkItemRepository }
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import java.time.{ Duration, Instant, LocalDateTime }
//import java.util.UUID
//import javax.inject.Inject
//
//class SubscriberQueuesRepositorySpec @Inject() extends PlaySpec with GuiceOneAppPerSuite with MockitoSugar {
//
//  "addWorkItem" must {
//    "return true" in new TestCase {
//      await(
//        subscriberQueuesRepo
//          .addWorkItem(clientSession, repository, eventWorkItem)
//          .toFuture()
//          .map(_.wasAcknowledged())) mustBe true
//    }
//  }
//
//  class TestCase {
//    val mongo = app.injector.instanceOf[MongoComponent]
//    val eventId = UUID.randomUUID().toString
//    val repository = new WorkItemRepository[Event](
//      "subscriberName",
//      mongo,
//      Event.eventFormat,
//      WorkItemFields.default,
//      false
//    ) {
//      override def inProgressRetryAfter: Duration = Duration.ofSeconds(1)
//      override def now(): Instant = Instant.now()
//    }
//
//    val subscriberQueuesRepo = new SubscriberQueuesRepository()
//
//    val event =
//      Event(UUID.fromString(eventId), "sub", "group", LocalDateTime.MIN, Json.parse("""{"reason":"email not valid"}"""))
//
//    val eventWorkItem = WorkItem(
//      id = new ObjectId(),
//      receivedAt = Instant.now(),
//      updatedAt = Instant.now(),
//      availableAt = Instant.now(),
//      status = ProcessingStatus.ToDo,
//      failureCount = 0,
//      item = event
//    )
//    val clientSession: ClientSession = await(mongo.client.startSession().head())
//  }
//}
