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

import cats.syntax.either._
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eventhub.config.TestModels._
import uk.gov.hmrc.eventhub.model.TestModels._
import uk.gov.hmrc.eventhub.model.{DuplicateEvent, Event, NoSubscribersForTopic, SubscriberRepository}
import uk.gov.hmrc.eventhub.repository.EventRepository
import uk.gov.hmrc.mongo.workitem.WorkItemRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EventPublisherServiceImplSpec extends AnyFlatSpec with Matchers with IdiomaticMockito with ScalaFutures {

  behavior of "EventPublisherServiceImpl.publish"

  it should "return a set of subscribers that were successfully published to" in new Scope {
    eventRepository.find(event.eventId) returns Future.successful(None)
    subscriptionMatcher.apply(event, EmailTopic) returns Set(
      SubscriberRepository(EmailTopic, channelPreferences, mock[WorkItemRepository[Event]])
    ).asRight
    eventPublisher.apply(*, *) returns Future.successful(Set(channelPreferences).asRight)

    eventPublisherServiceImpl
      .publish(event, EmailTopic)
      .futureValue shouldBe Set(channelPreferences).asRight
  }

  it should "return a DuplicateEvent error when the event has already been published" in new Scope {
    eventRepository.find(event.eventId) returns Future.successful(Some(event))

    eventPublisherServiceImpl
      .publish(event, EmailTopic)
      .futureValue shouldBe DuplicateEvent(s"Duplicate Event: Event with eventId already exists").asLeft
  }

  it should "return a PublishError returned from SubscriptionMatcher" in new Scope {
    val noSubscribersForTopic: NoSubscribersForTopic = NoSubscribersForTopic("No subscribers for topic")
    eventRepository.find(event.eventId) returns Future.successful(None)
    subscriptionMatcher.apply(event, EmailTopic) returns noSubscribersForTopic.asLeft

    eventPublisherServiceImpl
      .publish(event, EmailTopic)
      .futureValue shouldBe noSubscribersForTopic.asLeft
  }

  trait Scope {
    val eventRepository: EventRepository = mock[EventRepository]
    val subscriptionMatcher: SubscriptionMatcher = mock[SubscriptionMatcher]
    val eventPublisher: EventPublisher = mock[EventPublisher]

    val eventPublisherServiceImpl: EventPublisherServiceImpl = new EventPublisherServiceImpl(
      eventRepository,
      subscriptionMatcher,
      eventPublisher
    )
  }
}
