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

package uk.gov.hmrc.eventhub.subscriptions.http

import akka.http.scaladsl.model.StatusCodes
import play.api.Logging
import uk.gov.hmrc.eventhub.model.{Event, Subscriber}
import uk.gov.hmrc.eventhub.respository.SubscriberEventRepository
import uk.gov.hmrc.eventhub.subscriptions.http.HttpResponseHandler.{EventSendStatus, Failed, SendStatus, Sent}
import uk.gov.hmrc.eventhub.subscriptions.stream.SubscriberEventHttpFlow.SubscriberEventHttpResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object HttpResponseHandler {
  val ResponseParallelism = 4;

  sealed trait SendStatus

  case object Sent extends SendStatus
  case object Failed extends SendStatus

  case class EventSendStatus(event: Event, subscriber: Subscriber, sendStatus: SendStatus)
}

trait HttpResponseHandler {
  def handleResponse(subscriberEventHttpResult: SubscriberEventHttpResponse): Future[EventSendStatus]
}

class EventMarkingHttpResponseHandler(
  subscriberEventRepository: SubscriberEventRepository
)(implicit executionContext: ExecutionContext) extends HttpResponseHandler with Logging {

  def handleResponse(subscriberEventHttpResult: SubscriberEventHttpResponse): Future[EventSendStatus] = subscriberEventHttpResult match {
    case SubscriberEventHttpResponse(response, event, subscriber) =>
      val resultF = EventSendStatus(event, subscriber, _)

      response match {
        case Failure(e) =>
          logger.error(s"could not push event: $event to: ${subscriber.uri}, marking as failed.", e)
          markAsFailed(event, resultF)

        case Success(response) =>
          response.status match {
          case StatusCodes.Success(_) => subscriberEventRepository.sent(event).map(_ => resultF(Sent))

          case StatusCodes.ClientError(_) =>
            logger.error(s"client error: ${response.status} when pushing: $event to: ${subscriber.uri}.")
            markAsFailed(event, resultF)

          case _ =>
            logger.error(s"upstream error: ${response.status} when pushing: $event to: ${subscriber.uri}.")
            markAsFailed(event, resultF)
        }
      }
  }

  private def markAsFailed(event: Event, resultF: SendStatus => EventSendStatus): Future[EventSendStatus] =
    subscriberEventRepository.failed(event).map(_ => resultF(Failed))

}