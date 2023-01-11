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

package uk.gov.hmrc.eventhub.subscription.http

import cats.syntax.option._
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, RequestTimeoutException, StatusCodes}
import akka.stream.Materializer
import uk.gov.hmrc.eventhub.config.Subscriber
import uk.gov.hmrc.eventhub.metric.MetricsReporter
import uk.gov.hmrc.eventhub.metric.MetricsReporter.{ExceptionalStatus, HttpStatus}
import uk.gov.hmrc.eventhub.model.Event

import scala.util.{Failure, Success, Try}

trait HttpRetryHandler {
  def shouldRetry(input: (HttpRequest, Event), output: (Try[HttpResponse], Event)): Option[(HttpRequest, Event)]
}

class HttpRetryHandlerImpl(
  subscriber: Subscriber,
  metricsReporter: MetricsReporter
)(implicit materializer: Materializer)
    extends HttpRetryHandler {
  override def shouldRetry(
    input: (HttpRequest, Event),
    output: (Try[HttpResponse], Event)
  ): Option[(HttpRequest, Event)] =
    output match {
      case (Success(resp), _) =>
        resp.entity.discardBytes()
        resp.status match {
          case StatusCodes.ServerError(_) | StatusCodes.TooManyRequests =>
            metricsReporter.incrementSubscriptionRetry(subscriber, HttpStatus(resp.status))
            input.some
          case _ => none
        }
      case (Failure(ex), _) =>
        ex match {
          case _: RequestTimeoutException =>
            metricsReporter.incrementSubscriptionRetry(subscriber, ExceptionalStatus)
            input.some
          case e: RuntimeException if e.getMessage.contains("The http server closed the connection unexpectedly") =>
            metricsReporter.incrementSubscriptionRetry(subscriber, ExceptionalStatus)
            input.some
          case _ => none
        }
    }
}
