/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.eventhub.repository

import org.mockito.IdiomaticMockito
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import play.api.Configuration
import uk.gov.hmrc.eventhub.config.TopicName
import uk.gov.hmrc.eventhub.model.TestModels.{channelPreferences, event}
import uk.gov.hmrc.mongo.MongoComponent
import play.api.libs.ws.WSClient
import uk.gov.hmrc.integration.ServiceSpec
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.{InProgress, PermanentlyFailed, ToDo}

import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class SubscriberQueueRepositorySpec
    extends AnyFlatSpec with Matchers with IdiomaticMockito with ScalaFutures with ServiceSpec {

  lazy val ttlInSecondsEvent = 300
  lazy val ttlInSecondsSubscribers = 300

  override def externalServices: Seq[String] = Seq.empty[String]

  override def additionalConfig: Map[String, Any] =
    Map(
      "application.router"                        -> "testOnlyDoNotUseInAppConf.Routes",
      "metrics.enabled"                           -> false,
      "auditing.enabled"                          -> false,
      "queue.retryFailedAfter"                    -> "1 second",
      "queue.numberOfRetries"                     -> "3",
      "event-repo.expire-after-seconds-ttl"       -> ttlInSecondsEvent,
      "subscriber-repos.expire-after-seconds-ttl" -> ttlInSecondsSubscribers
    )

  behavior of "SubscriberQueueRepository.now"

  it should "return instant now value" in new Scope {
    val oldNow: Long = Instant.now().toEpochMilli
    val sutNow: Long = subscriberQueueRepository.now().toEpochMilli
    val newNow: Long = Instant.now().toEpochMilli
    (oldNow <= sutNow && sutNow >= newNow) should be(true)
  }

  behavior of "SubscriberQueueRepository.getEvent"

  it should "return an event when the underlying repo successfully finds one for that event and changes its status to InProgress" in new Scope {
    subscriberQueueRepository.collection.deleteMany(BsonDocument()).toFuture()
    subscriberQueueRepository.pushNew(event).futureValue
    val eventWorkItem = subscriberQueueRepository.getEvent.futureValue
    eventWorkItem.get.item should be(event)
    eventWorkItem.get.status should be(InProgress)
  }

  behavior of "SubscriberQueueRepository.findAsWorkItem"

  it should "return an event when the underlying repo successfully finds one for that event" in new Scope {
    subscriberQueueRepository.collection.deleteMany(BsonDocument()).toFuture()
    subscriberQueueRepository.pushNew(event).futureValue
    val eventWorkItem = subscriberQueueRepository.findAsWorkItem(event).futureValue
    eventWorkItem.get.item should be(event)
    eventWorkItem.get.status should be(ToDo)
  }

  behavior of "SubscriberQueueRepository.permanentlyFailed"

  it should "return an event when the underlying repo successfully finds one for that event" in new Scope {
    subscriberQueueRepository.collection.deleteMany(BsonDocument()).toFuture()
    subscriberQueueRepository.pushNew(event).futureValue

    val eventWorkItem1 = subscriberQueueRepository.getEvent.futureValue
    eventWorkItem1.get.item should be(event)
    eventWorkItem1.get.status should be(InProgress)

    subscriberQueueRepository.permanentlyFailed(eventWorkItem1.get).futureValue

    val eventWorkItem2 = subscriberQueueRepository.findAsWorkItem(event).futureValue
    eventWorkItem2.get.item should be(event)
    eventWorkItem2.get.status should be(PermanentlyFailed)
  }

  behavior of "SubscriberQueueRepository.failed"

  it should "mark a work item as permanently failed if it has reached the numberOfRetries (3x) threshold" in new Scope {
    val numberOfRetries = 3
    // ensuring threshold is configured to 3
    subscriberQueueRepository.numberOfRetries should be(numberOfRetries)

    subscriberQueueRepository.collection.deleteMany(BsonDocument()).toFuture()
    subscriberQueueRepository.pushNew(event).futureValue

    (0 to (numberOfRetries + 1)).foreach { failureCount =>
      tenSeconds {
        val eventWorkItem = subscriberQueueRepository.getEvent.futureValue
        eventWorkItem.get.item should be(event)
        eventWorkItem.get.status should be(InProgress)
        eventWorkItem.get.failureCount should be(failureCount)
        subscriberQueueRepository.failed(eventWorkItem.get).futureValue
      }
    }

    tenSeconds {
      val finalEventWorkItem = subscriberQueueRepository.findAsWorkItem(event).futureValue
      finalEventWorkItem.get.item should be(event)
      finalEventWorkItem.get.status should be(PermanentlyFailed)
    }
  }

  trait Scope {
    implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
    val wsClient: WSClient = app.injector.instanceOf[WSClient]
    val mongoComponent: MongoComponent = app.injector.instanceOf[MongoComponent]
    val configuration: Configuration = app.injector.instanceOf[Configuration]
    val topicName: TopicName = TopicName(name = "topicName")

    val subscriberQueueRepository: SubscriberQueueRepository =
      new SubscriberQueueRepository(topicName, channelPreferences, configuration, mongoComponent)

    def tenSeconds[T](fun: => T): T = eventually(timeout(10.seconds), interval(100.milliseconds))(fun)
  }
}
