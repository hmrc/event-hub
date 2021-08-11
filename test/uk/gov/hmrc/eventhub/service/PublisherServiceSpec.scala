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
import org.mockito.ArgumentMatchersSugar.any
import org.mockito.Mockito.when
import org.mockito.MockitoSugar.mock
import org.mongodb.scala.MongoClient
import org.scalatest.matchers.must.Matchers
import org.scalatest.time.SpanSugar.convertIntToGrainOfTime
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.eventhub.config.TestModels.{Elements, subscriber}
import uk.gov.hmrc.eventhub.config.{Subscriber, Topic}
import uk.gov.hmrc.eventhub.helpers.Resources
import uk.gov.hmrc.eventhub.model._
import uk.gov.hmrc.eventhub.modules.MongoSetup
import uk.gov.hmrc.eventhub.repository.{EventRepository, SubscriberQueuesRepository}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.workitem.WorkItemRepository
import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PublisherServiceSpec extends AnyWordSpec with Matchers {

  "subscriberReposFiltered" must {
    "return only repositories that match pathFilter" in new TestCase {
      val subscriberReposWithoutFilter = Set(
        SubscriberRepository("email", subscriberSetUp("subscriber1"), subscriberItemsRepo),
        SubscriberRepository("email", subscriberSetUp("subscriber2"), subscriberItemsRepo),
        SubscriberRepository("email", subscriberSetUp("subscriber3"), subscriberItemsRepo),
        SubscriberRepository("dummy", subscriberSetUp("subscriber4"), subscriberItemsRepo)
      )

      val subscriberReposWithPathFilter = Seq(
        subscriberSetUp(
          "subscriber2",
          Some(JsonPath.compile("$.event[?(@.enrolment =~ /HMRC\\-CUS\\-ORG\\~EORINumber~.*/i)]"))
        ),
        subscriberSetUp(
          "subscriber3",
          Some(JsonPath.compile("$.event[?(@.enrolment =~ /HMRC\\-CUS\\-ORG\\~EORINumber~.*/i)]"))
        )
      )

      when(mongoSetup.subscriberRepositories).thenReturn(subscriberReposWithoutFilter)

      val publisherService =
        new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)

      publisherService.subscriberReposFiltered("email", subscriberReposWithPathFilter).size mustBe 2
      publisherService
        .subscriberReposFiltered("email", subscriberReposWithPathFilter)
        .map(_.subscriber)
        .map(_.name) mustBe Set("subscriber2", "subscriber3")
    }

    "return empty if unknown topic" in new TestCase {
      val subscriberReposWithoutFilter = Set(
        SubscriberRepository("unknown", subscriberSetUp("invalidPath"), subscriberItemsRepo)
      )

      val subscriberReposWithPathFilter = Seq(
        subscriberSetUp(
          "invalidPath",
          Some(JsonPath.compile("$.event[?(@.enrolment =~ /H\\-C\\-ORG\\~EORINumber~.*/i)]"))
        )
      )

      when(mongoSetup.subscriberRepositories).thenReturn(subscriberReposWithoutFilter)

      val publisherService =
        new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)

      publisherService.subscriberReposFiltered("email", subscriberReposWithPathFilter).size mustBe 0
    }

    "return empty repositories if filtered subscribers are empty" in new TestCase {
      when(mongoSetup.subscriberRepositories).thenReturn(
        Set(SubscriberRepository("email", subscriber, subscriberItemsRepo))
      )
      val publisherService =
        new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)
      publisherService.subscriberReposFiltered("email", Seq(subscriberSetUp("subscriber1"))) mustBe Set()
    }

  }

  "matchingSubscribers" must {
    "return only subscribers that match filterPath in config" in new TestCase {
      val subscriber1 = subscriberSetUp("subscriber1")
      val subscriber2 = subscriberSetUp(
        "subscriber2",
        Some(JsonPath.compile("$.event[?(@.enrolment =~ /HMRC\\-CUS\\-ORG\\~EORINumber~.*/i)]"))
      )
      val subscriberInvalidPath = subscriberSetUp(
        "subscriber3",
        Some(JsonPath.compile("$.event[?(@.en =~ /HMRC\\-CUS\\-ORG\\~EORINumber~.*/i)]"))
      )
      val subscriberInvalidPath2 =
        subscriberSetUp("subscriber3", Some(JsonPath.compile("$.event[?(@.en =~ /HMRC\\-ORG\\~EORINumber~.*/i)]")))

      val subscribers = List(subscriber1, subscriber2, subscriberInvalidPath, subscriberInvalidPath2)

      val publisherService =
        new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)
      publisherService.matchingSubscribers(event, subscribers) mustBe Seq(subscriber2)
    }
  }

  "publishIfUnique" must {
    "return DuplicateEvent if Event Already exists" in new TestCase {
      when(eventRepository.find(any[String], any[PlayMongoRepository[Event]])).thenReturn(Future.successful(Seq(event)))
      val publisherService =
        new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)
      await(publisherService.publishIfUnique("email", event)) mustBe Left(
        DuplicateEvent("Duplicate Event: Event with eventId already exists")
      )
    }
    "return NoEventTopic if topic doesn't exist" in new TestCase {
      when(eventRepository.find(any[String], any[PlayMongoRepository[Event]])).thenReturn(Future.successful(Seq()))
      when(mongoSetup.topics).thenReturn(Set(Topic("not exist", List())))
      val publisherService =
        new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)
      await(publisherService.publishIfUnique("email", event)) mustBe Left(NoEventTopic("No such topic"))
    }
    "return NoSubscribersForTopic if subscribers are empty" in new TestCase {
      when(eventRepository.find(any[String], any[PlayMongoRepository[Event]])).thenReturn(Future.successful(Seq()))

      when(mongoSetup.topics).thenReturn(Set(Topic("email", List())))
      val publisherService =
        new PublisherService(mongoComponent, eventRepository, subscriberQueuesRepository, mongoSetup)
      await(publisherService.publishIfUnique("email", event)) mustBe Left(
        NoSubscribersForTopic("No subscribers for topic")
      )
    }

  }

//  class PublisherServiceSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite {
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
    when(mongoComponent.client).thenReturn(mongoClient)

    val eventId = UUID.randomUUID().toString

    val event = Resources.readJson("valid-event.json").as[Event]

    val eventWithOutValidPath = Event(
      UUID.fromString(eventId),
      "sub",
      "group",
      LocalDateTime.MIN,
      Json.parse("""{"enrolment":"HMRC-OOO-ORG~EORINumber~AB1234567891"}""")
    )
    val eventWithOutPath = Event(UUID.fromString(eventId), "sub", "group", LocalDateTime.MIN, Json.parse("""{}"""))
  }

  def subscriberSetUp(name: String, pathFilter: Option[JsonPath] = None): Subscriber =
    Subscriber(
      name = name,
      uri = Uri(s"http://localhost/path"),
      httpMethod = HttpMethods.POST,
      elements = Elements,
      per = 3.seconds,
      minBackOff = 10.millis,
      maxBackOff = 1.second,
      maxRetries = 2,
      pathFilter = pathFilter,
      maxConnections = 1
    )
}
