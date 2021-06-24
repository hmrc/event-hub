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
//
//package uk.gov.hmrc.eventhub.service
//
//import com.mongodb.client.result.InsertOneResult
//import org.bson.{BsonNull, BsonValue}
//import org.mockito.Matchers.any
//import org.mockito.Mockito.{times, verify, when}
//import org.mongodb.scala.result.InsertOneResult
//import org.mongodb.scala.{ClientSession, MongoClient, Observable, SingleObservable}
//import org.scalatest.matchers.must.Matchers
//import org.scalatest.wordspec.AnyWordSpec
//import org.scalatestplus.mockito.MockitoSugar.mock
//import play.api.libs.json.Json
//import play.api.test.Helpers.await
//import uk.gov.hmrc.eventhub.models.{DuplicateEvent, Event, NoEventTopic, NoSubscribersForTopic, PublishError, Subscriber}
//import uk.gov.hmrc.eventhub.modules.MongoSetup
//import uk.gov.hmrc.eventhub.repository.{EventRepository, SubscriberQueuesRepository}
//import uk.gov.hmrc.mongo.MongoComponent
//import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
//import uk.gov.hmrc.mongo.workitem.{WorkItem, WorkItemFields, WorkItemRepository}
//import play.api.test.Helpers.{await, defaultAwaitTimeout}
//
//import java.time.{Duration, Instant, LocalDateTime}
//import java.util.UUID
//import scala.concurrent.ExecutionContext.Implicits.global
//import scala.concurrent.Future
//
//class PublisherServiceSpec extends AnyWordSpec with Matchers {
//
//private  val mongoComponent = mock[MongoComponent]
//private  val eventRepository = mock[EventRepository]
//private  val subscriberQueuesRepository = mock[SubscriberQueuesRepository]
//private  val mongoSetup = mock[MongoSetup]
//
//private  val subscriberItemsRepo = mock[WorkItemRepository[Event]]
//private  val mongoClient: MongoClient = MongoClient()
//
//  when(mongoSetup.topics).thenReturn(Map("email" -> List(Subscriber("name", "uri"))))
//  when(mongoSetup.subscriberRepositories).thenReturn(Seq(("email", subscriberItemsRepo), ("email", subscriberItemsRepo)))
//  when(mongoComponent.client).thenReturn(mongoClient)
//
//  val eventId = UUID.randomUUID().toString
//  val event = Event(UUID.fromString(eventId), "sub", "group", LocalDateTime.MIN,
//    Json.parse("""{"reason":"email not valid"}"""))
//
//  when(eventRepository.find(any[String], any[PlayMongoRepository[Event]])).thenReturn(Future.successful(Seq.empty[Event]))
//
//
//"publishIfUnique" must {
//"return DuplicateEvent if Event Already exists" in {
//  when(eventRepository.find(any[String], any[PlayMongoRepository[Event]])).thenReturn(Future.successful(Seq(event)))
//  val publisherService = new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)
// await(publisherService.publishIfUnique("email", event)) mustBe Left(DuplicateEvent("Duplicate Event: Event with eventId already exists"))
//}
//
//  "return NoEventTopic if topic doesn't exist" in {
//    when(mongoSetup.topics).thenReturn(Map("not exist" -> List()))
//        val publisherService = new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)
//    await(publisherService.publishIfUnique("email", event)) mustBe Left(NoEventTopic("No such topic"))
//  }
//
//  "return NoSubscribersForTopic if subscribers are empty" in {
//    when(mongoSetup.topics).thenReturn(Map("email" -> List()))
//    val publisherService = new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)
//    await(publisherService.publishIfUnique("email", event)) mustBe Left(NoSubscribersForTopic("No subscribers for topic"))
//  }
//
////  "return Right" in {
////    when(mongoSetup.topics).thenReturn(Map("email" -> List(Subscriber("name", "uri"))))
////
////
////    val insertOneResult = new InsertOneResultAcc
////    when(eventRepository.addEvent(any[ClientSession], any[PlayMongoRepository[Event]], any[Event])).thenReturn(SingleObservable(insertOneResult))
////    when(subscriberQueuesRepository.addWorkItem(any[ClientSession],
////      any[WorkItemRepository[Event]], any[WorkItem[Event]])).thenReturn(SingleObservable(insertOneResult))
////
////
////    val publisherService = new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)
////    await(publisherService.publishIfUnique("email", event)) mustBe Left(NoSubscribersForTopic("No subscribers for topic"))
////  }
//
//}
//}
//
//class InsertOneResultImpTest extends InsertOneResult {
//  override def wasAcknowledged(): Boolean = true
//
//  override def getInsertedId: BsonValue = BsonNull.VALUE
//}
//
//private class InsertOneResultAcc extends InsertOneResult {
//  override def wasAcknowledged: Boolean = false
//
// override def getInsertedId: BsonValue = throw new UnsupportedOperationException("Cannot get information about an unacknowledged insert")
//
//  override def equals(o: Any): Boolean = {
//
//    true
//  }
//
//  override def hashCode: Int = 0
//
//  override def toString: String = "UnacknowledgedInsertOneResult{}"
//}