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

import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes }
import akka.stream.Materializer
import play.api.Logging
import uk.gov.hmrc.eventhub.model.Event

import scala.util.{ Failure, Success, Try }

trait HttpRetryHandler extends Logging {
  def shouldRetry()(implicit materializer: Materializer): ((HttpRequest, Event), (Try[HttpResponse], Event)) => Option[(HttpRequest, Event)] = {
    case (inputs @ (request, _), (Success(resp), _)) =>
      resp.entity.discardBytes()
      resp.status match {
        case StatusCodes.Success(_) | StatusCodes.ClientError(_) =>
          logger.info(s"\nresponse status:\n ${resp.status}.\nfor request:\n $request.\nwill not retry.")
          None
        case _ => Some(inputs)
      }
    case ((_, _), (Failure(_), _)) => None
  }
}

object HttpRetryHandler extends HttpRetryHandler
