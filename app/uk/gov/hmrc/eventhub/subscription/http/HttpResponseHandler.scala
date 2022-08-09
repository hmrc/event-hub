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

package uk.gov.hmrc.eventhub.subscription.http

import akka.http.scaladsl.model.StatusCodes
import play.api.Logging
import uk.gov.hmrc.eventhub.model.Event
import uk.gov.hmrc.eventhub.subscription.stream.SubscriberEventHttpResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import HttpResponseHandler._
import uk.gov.hmrc.eventhub.config.Subscriber
import uk.gov.hmrc.eventhub.metric.MetricsReporter
import uk.gov.hmrc.eventhub.repository.SubscriberEventRepository

class HttpResponseHandler(
  subscriberEventRepository: SubscriberEventRepository,
  metricsReporter: MetricsReporter
)(implicit executionContext: ExecutionContext)
    extends Logging {

  def handle(subscriberEventHttpResponse: SubscriberEventHttpResponse): Future[EventSendStatus] =
    subscriberEventHttpResponse match {
      case SubscriberEventHttpResponse(response, event, subscriber) =>
        val resultF = EventSendStatus(event, subscriber, _)
        metricsReporter.stopSubscriptionPublishTimer(subscriber, event)

        response match {
          case Failure(e) =>
            logger.error(s"could not push event: $event to: ${subscriber.uri}, marking as failed.", e)
            markAsFailed(event, subscriber, resultF)

          case Success(response) =>
            response.status match {
              case StatusCodes.Success(_) =>
                logger.debug(s"pushed event: $event to: ${subscriber.uri}, marking as sent.")
                metricsReporter.incrementSubscriptionPublishedCount(subscriber)
                subscriberEventRepository.sent(event).map(_ => resultF(Sent))

              case StatusCodes.TooManyRequests =>
                logger.warn(s"rate limit error: ${response.status} when pushing: $event to: ${subscriber.uri}.")
                markAsFailed(event, subscriber, resultF)

              case StatusCodes.ServerError(_) =>
                logger.warn(s"server error: ${response.status} when pushing: $event to: ${subscriber.uri}.")
                markAsFailed(event, subscriber, resultF)

              case StatusCodes.ClientError(_) =>
                logger.warn(s"client error: ${response.status} when pushing: $event to: ${subscriber.uri}.")
                remove(event, subscriber, resultF)

              case _ =>
                logger.warn(s"error: ${response.status} when pushing: $event to: ${subscriber.uri}.")
                remove(event, subscriber, resultF)
            }
        }
    }

  private def markAsFailed(
    event: Event,
    subscriber: Subscriber,
    resultF: SendStatus => EventSendStatus
  ): Future[EventSendStatus] = {
    metricsReporter.incrementSubscriptionFailure(subscriber)
    subscriberEventRepository.failed(event).map(_ => resultF(Failed))
  }

  private def remove(
    event: Event,
    subscriber: Subscriber,
    resultF: SendStatus => EventSendStatus
  ): Future[EventSendStatus] = {
    metricsReporter.incrementSubscriptionPermanentFailure(subscriber)
    subscriberEventRepository.remove(event).map(_ => resultF(Removed))
  }
}

object HttpResponseHandler {
  sealed trait SendStatus

  case object Sent extends SendStatus
  case object Failed extends SendStatus
  case object Removed extends SendStatus

  case class EventSendStatus(event: Event, subscriber: Subscriber, sendStatus: SendStatus)
}
