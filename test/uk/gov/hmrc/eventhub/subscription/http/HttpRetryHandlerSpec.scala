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

import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpMethods.POST
import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError, OK}
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eventhub.model.Event
import uk.gov.hmrc.eventhub.model.TestModels._
import uk.gov.hmrc.eventhub.config.TestModels._

import scala.util.{Failure, Success, Try}

class HttpRetryHandlerSpec extends AnyFlatSpec with Matchers {

  behavior of "HttpRetryHandler.shouldRetry"

  it should "return `None` when a http response is provided with a status code in the 200 range" in new Scope {
    shouldRetry(httpRequest -> event, Success(successfulHttpResponse) -> event) shouldBe None
  }

  it should "return `None` when a http response is provided with a status code in the 400 range" in new Scope {
    shouldRetry(httpRequest -> event, Success(clientErrorHttpResponse) -> event) shouldBe None
  }

  it should "return `None` when a http response could not be provided due to an exception" in new Scope {
    shouldRetry(httpRequest -> event, Failure(new IllegalStateException("boom boom")) -> event) shouldBe None
  }

  it should "return Some(inputs) when a http response is provided with a status in the 500 range" in new Scope {
    shouldRetry(httpRequest -> event, Success(internalServerErrorHttpResponse) -> event) shouldBe Some(
      httpRequest           -> event
    )
  }

  trait Scope {
    private val system: ActorSystem = ActorSystem()
    private val materializer: Materializer = Materializer(system)

    val httpRequest: HttpRequest = HttpRequest(method = POST, uri = subscriber.uri)
    val successfulHttpResponse: HttpResponse = HttpResponse(status = OK)
    val clientErrorHttpResponse: HttpResponse = HttpResponse(status = BadRequest)
    val internalServerErrorHttpResponse: HttpResponse = HttpResponse(status = InternalServerError)

    def shouldRetry: ((HttpRequest, Event), (Try[HttpResponse], Event)) => Option[(HttpRequest, Event)] =
      HttpRetryHandler.shouldRetry()(materializer)
  }
}
