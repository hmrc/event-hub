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

package uk.gov.hmrc.eventhub.events

import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.SingleObservableFuture
import org.scalatest.concurrent.Eventually.eventually
import uk.gov.hmrc.eventhub.ISpec
import uk.gov.hmrc.eventhub.config.TransactionConfiguration.sessionOptions
import uk.gov.hmrc.eventhub.config.TopicName
import uk.gov.hmrc.eventhub.model.Event
import uk.gov.hmrc.eventhub.repository.EventRepository
import uk.gov.hmrc.eventhub.subscription._
import uk.gov.hmrc.eventhub.subscription.model.TestModels.Events.eventJson

import scala.concurrent.duration._
import uk.gov.hmrc.eventhub.subscription.model.TestModels.Subscriptions.channelPreferences

import java.time.LocalDateTime
import java.util.UUID

class EventRepositoryISpec extends ISpec {

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
  override def additionalConfig: Map[String, ? <: Any] =
    Map(
      "application.router"                        -> "app.Routes",
      "metrics.enabled"                           -> false,
      "auditing.enabled"                          -> false,
      "event-repo.expire-after-seconds-ttl"       -> ttlInSecondsEvent,
      "subscriber-repos.expire-after-seconds-ttl" -> ttlInSecondsSubscribers,
      "topics"                                    -> channelPreferences.asConfigMap(TopicName("email"))
    )

  "eventRepository" should {

    "add an event through the repository and allow to retrieve it from the collection" in new Scope {
      for {
        _                  <- repo.collection.deleteMany(BsonDocument()).toFuture()
        clientSession      <- mongoComponent.client.startSession(sessionOptions).toFuture()
        insertAcknowledged <- eventRepository.addEvent(clientSession, event).map(_.wasAcknowledged())
        count              <- repo.collection.countDocuments().toFuture()
        eventRead          <- eventRepository.find(event.eventId)
      } yield {
        insertAcknowledged must be(true)
        count must be(1)
        eventRead must be(event)
      }
    }
  }

  trait Scope {
    val eventRepository = new EventRepository(mongoSetup.eventRepository)
    val repo = mongoSetup.eventRepository
  }
}
