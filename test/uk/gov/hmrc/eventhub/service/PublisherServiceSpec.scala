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


package uk.gov.hmrc.eventhub.service

import com.mongodb.client.result.InsertOneResult
import org.bson.{BsonNull, BsonValue}
import org.mockito.Matchers.any
import org.mockito.Mockito.{times, verify, when}
import org.mongodb.scala.result.InsertOneResult
import org.mongodb.scala.{ClientSession, MongoClient, Observable, SingleObservable}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.eventhub.models.{DuplicateEvent, Event, NoEventTopic, NoSubscribersForTopic, PublishError, Subscriber}
import uk.gov.hmrc.eventhub.modules.MongoSetup
import uk.gov.hmrc.eventhub.repository.{EventRepository, SubscriberQueuesRepository}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.workitem.{WorkItem, WorkItemFields, WorkItemRepository}
import play.api.test.Helpers.{await, defaultAwaitTimeout}

import java.time.{Duration, Instant, LocalDateTime}
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PublisherServiceSpec extends AnyWordSpec with Matchers {

"publishIfUnique" must {
"return DuplicateEvent if Event Already exists" in new TestCase {
  when(eventRepository.find(any[String], any[PlayMongoRepository[Event]])).thenReturn(Future.successful(Seq(event)))
  val publisherService = new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)
 await(publisherService.publishIfUnique("email", event)) mustBe Left(DuplicateEvent("Duplicate Event: Event with eventId already exists"))
}

  "return NoEventTopic if topic doesn't exist" in new TestCase {
    when(eventRepository.find(any[String], any[PlayMongoRepository[Event]])).thenReturn(Future.successful(Seq()))
    when(mongoSetup.topics).thenReturn(Map("not exist" -> List()))
        val publisherService = new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)
    await(publisherService.publishIfUnique("email", event)) mustBe Left(NoEventTopic("No such topic"))
  }

  "return NoSubscribersForTopic if subscribers are empty" in new TestCase{
    when(eventRepository.find(any[String], any[PlayMongoRepository[Event]])).thenReturn(Future.successful(Seq()))

    when(mongoSetup.topics).thenReturn(Map("email" -> List()))
    val publisherService = new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)
    await(publisherService.publishIfUnique("email", event)) mustBe Left(NoSubscribersForTopic("No subscribers for topic"))
  }

  class TestCase {
     val mongoComponent: MongoComponent = mock[MongoComponent]
     val eventRepository: EventRepository = mock[EventRepository]
     val subscriberQueuesRepository: SubscriberQueuesRepository = mock[SubscriberQueuesRepository]
     val mongoSetup: MongoSetup = mock[MongoSetup]
     val subscriberItemsRepo: WorkItemRepository[Event] = mock[WorkItemRepository[Event]]
     val mongoClient: MongoClient = MongoClient()

    when(mongoSetup.topics).thenReturn(Map("email" -> List(Subscriber("name", "uri"))))
    when(mongoSetup.subscriberRepositories).thenReturn(Seq(("email", subscriberItemsRepo), ("email", subscriberItemsRepo)))
    when(mongoComponent.client).thenReturn(mongoClient)

    val eventId = UUID.randomUUID().toString
    val event = Event(UUID.fromString(eventId), "sub", "group", LocalDateTime.MIN,
      Json.parse("""{"reason":"email not valid"}"""))
  }
}
}