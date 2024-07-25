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

package uk.gov.hmrc.eventhub.service

import cats.syntax.either.*
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eventhub.config.TestModels.{channelPreferences, *}
import uk.gov.hmrc.eventhub.config.TopicName
import uk.gov.hmrc.eventhub.metric.MetricsReporter
import uk.gov.hmrc.eventhub.model.TestModels.*
import uk.gov.hmrc.eventhub.model.{DuplicateEvent, Event, NoSubscribersForTopic, SubscriberRepository}
import uk.gov.hmrc.eventhub.repository.EventRepository
import uk.gov.hmrc.mongo.workitem.WorkItemRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EventPublisherServiceImpSpec extends AnyFlatSpec with Matchers with MockitoSugar with ScalaFutures {

  behavior of "EventPublisherServiceImpl.publish"

  it should "return a set of subscribers that were successfully published to" in new Scope {
    when(eventRepository.find(event.eventId)).thenReturn(Future.successful(None))
    when(subscriptionMatcher(event, EmailTopic)).thenReturn(
      Set(
        SubscriberRepository(EmailTopic, channelPreferences, mock[WorkItemRepository[Event]])
      ).asRight
    )
    when(eventPublisher.apply(any, any)).thenReturn(Future.successful(()))

    eventPublisherServiceImpl
      .publish(event, EmailTopic)
      .futureValue shouldBe Set(channelPreferences).asRight

    verify(metricsReporter, times(1)).incrementEventPublishedCount(any, any[TopicName])
    verify(metricsReporter, times(1)).incrementSubscriptionEventEnqueuedCount(channelPreferences)
    verify(metricsReporter, times(1)).startSubscriptionPublishTimer(any, any)
  }

  it should "return a DuplicateEvent error when the event has already been published" in new Scope {
    when(eventRepository.find(event.eventId)).thenReturn(Future.successful(Some(event)))

    eventPublisherServiceImpl
      .publish(event, EmailTopic)
      .futureValue shouldBe DuplicateEvent(s"Duplicate Event: Event with eventId already exists").asLeft

    verify(metricsReporter, times(1)).incrementDuplicateEventCount(any, any[TopicName])
    verify(metricsReporter, never()).startSubscriptionPublishTimer(any, any)
  }

  it should "return a PublishError returned from SubscriptionMatcher" in new Scope {
    val noSubscribersForTopic: NoSubscribersForTopic = NoSubscribersForTopic("No subscribers for topic")
    when(eventRepository.find(event.eventId)).thenReturn(Future.successful(None))
    when(subscriptionMatcher.apply(event, EmailTopic)).thenReturn(noSubscribersForTopic.asLeft)

    eventPublisherServiceImpl
      .publish(event, EmailTopic)
      .futureValue shouldBe noSubscribersForTopic.asLeft

    verify(metricsReporter, never()).incrementEventPublishedCount(any, any[TopicName])
    verify(metricsReporter, never()).startSubscriptionPublishTimer(any, any)
  }

  trait Scope {
    val eventRepository: EventRepository = mock[EventRepository]
    val subscriptionMatcher: SubscriptionMatcher = mock[SubscriptionMatcherImpl]
    val eventPublisher: EventPublisher = mock[EventPublisher]
    val metricsReporter: MetricsReporter = mock[MetricsReporter]

    val eventPublisherServiceImpl: EventPublisherServiceImpl = new EventPublisherServiceImpl(
      eventRepository,
      subscriptionMatcher,
      eventPublisher,
      metricsReporter
    )
  }
}
