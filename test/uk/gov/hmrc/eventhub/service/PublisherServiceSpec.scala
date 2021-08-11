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

import akka.http.scaladsl.model.{HttpMethods, Uri}
import com.jayway.jsonpath.JsonPath
import org.mockito.Mockito.when
import org.mockito.MockitoSugar.mock
import org.mongodb.scala.MongoClient
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.eventhub.config.TestModels.Elements
import uk.gov.hmrc.eventhub.config.{Subscriber, Topic}
import uk.gov.hmrc.eventhub.helpers.Resources
import uk.gov.hmrc.eventhub.model.Event
import uk.gov.hmrc.eventhub.modules.MongoSetup
import uk.gov.hmrc.eventhub.repository.{EventRepository, SubscriberQueuesRepository}
import uk.gov.hmrc.eventhub.subscription.model.TestModels.Subscriptions
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.workitem.WorkItemRepository

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class PublisherServiceSpec extends AnyWordSpec with Matchers {
  "hasMandatoryPath" must {
    "return true if payload has matching path" in new TestCase {
      val publisherService =
        new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)
      publisherService.hasMandatoryPath(event, subscribers) mustBe true
    }

    "return false if event payload has no valid path" in new TestCase {
      val publisherService =
        new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)
      publisherService.hasMandatoryPath(eventWithOutValidPath, subscribers) mustBe false
    }

    "return false if payload has no path" in new TestCase {
      val publisherService =
        new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)
      publisherService.hasMandatoryPath(eventWithOutPath, subscribers) mustBe false
    }
  }
//  class PublisherServiceSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {
//    "publishIfUnique" must {
//      "return DuplicateEvent if Event Already exists" in new TestCase {
//        when(eventRepository.find(any[String], any[PlayMongoRepository[Event]])).thenReturn(Future.successful(Seq(event)))
//        val publisherService =
//          new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)
//        await(publisherService.publishIfUnique("email", event)) mustBe Left(DuplicateEvent("Duplicate Event: Event with eventId already exists"))
//      }
//
//      "return NoEventTopic if topic doesn't exist" in new TestCase {
//        when(eventRepository.find(any[String], any[PlayMongoRepository[Event]])).thenReturn(Future.successful(Seq()))
//        when(mongoSetup.topics).thenReturn(Set(Topic("not exist", List())))
//        val publisherService =
//          new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)
//        await(publisherService.publishIfUnique("email", event)) mustBe Left(NoEventTopic("No such topic"))
//      }
//
//      "return NoSubscribersForTopic if subscribers are empty" in new TestCase {
//        when(eventRepository.find(any[String], any[PlayMongoRepository[Event]])).thenReturn(Future.successful(Seq()))
//
//        when(mongoSetup.topics).thenReturn(Set(Topic("email", List())))
//        val publisherService =
//          new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)
//        await(publisherService.publishIfUnique("email", event)) mustBe Left(NoSubscribersForTopic("No subscribers for topic"))
//      }
//
//  ////TODO: This test is better if its mocked
//  //    "return success if publish is returned" in new TestCase {
//  //      val appEventRepository = app.injector.instanceOf[EventRepository]
//  //      val appMongoComponent = app.injector.instanceOf[MongoComponent]
//  //      val appSubscriberQueuesRepository = app.injector.instanceOf[SubscriberQueuesRepository]
//  //      val appMongoSetup = app.injector.instanceOf[MongoSetup]
//  //
//  //      val publisherService =
//  //        new PublisherService(appMongoComponent, appEventRepository, appSubscriberQueuesRepository, appMongoSetup)
//  //
//  //      val subscriberRepos = publisherService.subscriberRepos("email")
//  //
//  //      await(publisherService.publish(event, subscriberRepos).map(_ => ())) mustBe ()
//  //    }

  class TestCase {
    val mongoComponent: MongoComponent = mock[MongoComponent]
    val eventRepository: EventRepository = mock[EventRepository]
    val subscriberQueuesRepository: SubscriberQueuesRepository = mock[SubscriberQueuesRepository]
    val mongoSetup: MongoSetup = mock[MongoSetup]
    val subscriberItemsRepo: WorkItemRepository[Event] = mock[WorkItemRepository[Event]]
    val mongoClient: MongoClient = MongoClient()

    when(mongoSetup.topics).thenReturn(Set(Topic("email", List())))
    when(mongoSetup.subscriberRepositories)
      .thenReturn(Set(("email", subscriberItemsRepo), ("email", subscriberItemsRepo)))
    when(mongoComponent.client).thenReturn(mongoClient)

    val eventId = UUID.randomUUID().toString

    val event = Resources.readJson("valid-event.json").as[Event]

    val subscribers = List(
      Subscriber(
        name = Subscriptions.ChannelPreferencesBounced,
        uri = Uri(s"http://localhost$Subscriptions.ChannelPreferencesBouncedPath"),
        httpMethod = HttpMethods.POST,
        elements = Elements,
        per = 3.seconds,
        minBackOff = 10.millis,
        maxBackOff = 1.second,
        maxRetries = Subscriptions.MaxRetries,
        pathFilter = None,
        maxConnections = 1
      ),
      Subscriber(
        name = Subscriptions.ChannelPreferencesBounced,
        uri = Uri(s"http://localhost$Subscriptions.ChannelPreferencesBouncedPath"),
        httpMethod = HttpMethods.POST,
        elements = Elements,
        per = 3.seconds,
        minBackOff = 10.millis,
        maxBackOff = 1.second,
        maxRetries = Subscriptions.MaxRetries,
        pathFilter = Some(JsonPath.compile("$.event[?(@.enrolment =~ /HMRC\\-CUS\\-ORG\\~EORINumber~.*/i)]")),
        maxConnections = 1
      )
    )

    val eventWithOutValidPath = Event(
      UUID.fromString(eventId),
      "sub",
      "group",
      LocalDateTime.MIN,
      Json.parse("""{"enrolment":"HMRC-OOO-ORG~EORINumber~AB1234567891"}""")
    )
    val eventWithOutPath = Event(UUID.fromString(eventId), "sub", "group", LocalDateTime.MIN, Json.parse("""{}"""))
  }
}
