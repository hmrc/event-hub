/*
 * Copyright 2023 HM Revenue & Customs
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

import org.apache.pekko.http.scaladsl.model.HttpMethods
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.concurrent.Eventually.eventually
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.eventhub.config.{Subscriber, Topic, TopicName}
import uk.gov.hmrc.eventhub.model.{Event, PublishedEvent}
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.PermanentlyFailed
import uk.gov.hmrc.eventhub.subscription._
import uk.gov.hmrc.eventhub.subscription.model.TestModels.Events.eventJson

import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import uk.gov.hmrc.eventhub.subscription.model.TestModels.Subscriptions.{Elements, MaxConnections, MaxRetries, channelPreferences}

import java.time.LocalDateTime
import java.util.UUID

class EventHubModuleISpec extends ISpec {

  def threeMinutes[T](fun: => T): T = eventually(timeout(3.minutes), interval(500.milliseconds))(fun)

  lazy val ttlInSecondsEvent = 10
  lazy val ttlInSecondsSubscribers = 12
  val event: Event = Event(
    eventId = UUID.randomUUID(),
    subject = "bounced",
    groupId = "EventHubModuleSpec",
    timestamp = LocalDateTime.now(),
    event = eventJson
  )
  override def additionalConfig: Map[String, _ <: Any] =
    Map(
      "application.router"                        -> "testOnlyDoNotUseInAppConf.Routes",
      "metrics.enabled"                           -> false,
      "auditing.enabled"                          -> false,
      "event-repo.expire-after-seconds-ttl"       -> ttlInSecondsEvent,
      "subscriber-repos.expire-after-seconds-ttl" -> ttlInSecondsSubscribers,
      "topics"                                    -> channelPreferences.asConfigMap(TopicName("email"))
    )

  "Configuration" should {
    "include topics configuration1" in {
      mongoSetup.topics mustBe Set(
        Topic(
          TopicName("email"),
          List(
            Subscriber(
              "channel-preferences-bounced",
              "http://localhost/channel-preferences/process/bounce",
              HttpMethods.POST,
              Elements,
              3.seconds,
              MaxConnections,
              10.millis,
              1.second,
              MaxRetries,
              None
            )
          )
        )
      )
    }
  }

  "eventRepository" should {

    "create event repository" in {
      mongoSetup.eventRepository.collectionName mustBe "event"
    }

    "ensure index TTL applied to timestamp field" in {
      eventually {
        mongoSetup
          .eventRepository
          .indexes
          .find(idxModel => (idxModel.getOptions.getName == "createdAtTtlIndex"))
          .get
          .getOptions
          .getExpireAfter(TimeUnit.SECONDS) mustBe ttlInSecondsEvent
      }
    }

    "ensure inserted events are removed from mongo once TTL period set on timestamp field has been reached" in {
      val repo = mongoSetup.eventRepository
      val count = for {
        _          <- repo.collection.deleteMany(BsonDocument()).toFuture()
        _          <- repo.collection.insertOne(PublishedEvent.from(event)).toFuture()
        firstCount <- repo.collection.countDocuments().toFuture()
      } yield firstCount

      eventually {
        await(count) mustBe 1
      }

      threeMinutes {
        await(repo.collection.countDocuments().toFuture()) mustBe 0
      }
    }
  }

  "subscriberRepositories" should {
    "create subscriber repositories" in {
      mongoSetup.subscriberRepositories.map(_.workItemRepository.collectionName) mustBe
        Set("email_channel-preferences-bounced_queue")
    }

    "ensure inserted events which are now permanently failed are removed from mongo once TTL period set on updatedAt field has been reached" in {
      val repo = mongoSetup.subscriberRepositories.head.workItemRepository
      val result = for {
        _          <- repo.collection.deleteMany(BsonDocument()).toFuture()
        workItem   <- repo.pushNew(event)
        firstCount <- repo.collection.countDocuments().toFuture()
      } yield (workItem, firstCount)

      val (workItem, firstCount) = await(result)
      firstCount mustBe 1

      await(repo.markAs(workItem.id, PermanentlyFailed)) mustBe true

      threeMinutes {
        await(repo.collection.countDocuments().toFuture()) mustBe 0
      }
    }
  }
}
