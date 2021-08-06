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

package uk.gov.hmrc.eventhub.subscription.http

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.stream.Materializer
import uk.gov.hmrc.eventhub.model.Event

import javax.inject.{Inject, Singleton}
import scala.util.{Failure, Success, Try}

trait HttpRetryHandler {
  def shouldRetry(input: (HttpRequest, Event), output: (Try[HttpResponse], Event)): Option[(HttpRequest, Event)]
}

@Singleton
class HttpRetryHandlerImpl @Inject() (implicit materializer: Materializer) extends HttpRetryHandler {
  override def shouldRetry(
    input: (HttpRequest, Event),
    output: (Try[HttpResponse], Event)
  ): Option[(HttpRequest, Event)] =
    output match {
      case (Success(resp), _) =>
        resp.entity.discardBytes()
        resp.status match {
          case StatusCodes.ServerError(_) | StatusCodes.TooManyRequests => Some(input)
          case _                                                        => None
        }
      case (Failure(ex), _) =>
        ex match {
          case e: RuntimeException if e.getMessage.contains("The http server closed the connection unexpectedly") =>
            Some(input)
          case _ => None
        }
    }
}
