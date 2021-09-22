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

import uk.gov.hmrc.eventhub.config.{Subscriber, TopicName}
import uk.gov.hmrc.eventhub.model.{DuplicateEvent, Event, PublishError}
import uk.gov.hmrc.eventhub.repository.EventRepository
import cats.syntax.either._
import uk.gov.hmrc.eventhub.metric.MetricsReporter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EventPublisherServiceImpl @Inject() (
  eventRepository: EventRepository,
  subscriptionMatcher: SubscriptionMatcher,
  eventPublisher: EventPublisher,
  metricsReporter: MetricsReporter
)(implicit executionContext: ExecutionContext)
    extends EventPublisherService {
  def publish(event: Event, topicName: TopicName): Future[Either[PublishError, Set[Subscriber]]] =
    eventRepository.find(event.eventId).flatMap {
      case Some(_) =>
        metricsReporter.incrementDuplicateEventCount(event, topicName)
        Future.successful(DuplicateEvent(s"Duplicate Event: Event with eventId already exists").asLeft)
      case None => matchAndPublish(event, topicName)
    }

  private def matchAndPublish(event: Event, topicName: TopicName): Future[Either[PublishError, Set[Subscriber]]] =
    subscriptionMatcher(event, topicName) match {
      case Left(failure) => Future.successful(failure.asLeft)
      case Right(subscriberRepositories) =>
        val subscribers = subscriberRepositories.map(_.subscriber)
        eventPublisher(event, subscriberRepositories)
          .map(_ => reportPublish(event, topicName, subscribers))
          .map(_ => subscribers.asRight)
    }

  private def reportPublish(event: Event, topicName: TopicName, subscribers: Set[Subscriber]): Unit = {
    metricsReporter.incrementEventPublishedCount(event, topicName)
    subscribers.foreach(metricsReporter.incrementSubscriptionEventEnqueuedCount)
  }
}
