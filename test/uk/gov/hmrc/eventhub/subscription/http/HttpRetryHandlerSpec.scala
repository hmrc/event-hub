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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.model.HttpMethods.POST
import org.apache.pekko.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError, OK, TooManyRequests}
import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse, RequestTimeoutException}
import org.apache.pekko.stream.Materializer
import org.mockito.ArgumentMatchers.{eq => equalTo, *}
import org.mockito.Mockito.{times, verify}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eventhub.config.TestModels.*
import uk.gov.hmrc.eventhub.metric.MetricsReporter
import uk.gov.hmrc.eventhub.metric.MetricsReporter.{ExceptionalStatus, HttpStatus}
import uk.gov.hmrc.eventhub.model.Event
import uk.gov.hmrc.eventhub.model.TestModels.*

import scala.util.{Failure, Success, Try}

class HttpRetryHandlerSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  behavior of "HttpRetryHandler.shouldRetry"

  it should "return `None` when a http response is provided with a status code in the 200 range" in new Scope {
    shouldRetry(httpRequest -> event, Success(successfulHttpResponse) -> event) shouldBe None
  }

  it should "return `None` when a http response is provided with a status code of 400" in new Scope {
    shouldRetry(httpRequest -> event, Success(clientErrorHttpResponse) -> event) shouldBe None
  }

  it should "return `None` when a http response could not be provided due to an exception" in new Scope {
    shouldRetry(httpRequest -> event, Failure(new IllegalStateException("boom boom")) -> event) shouldBe None
  }

  it should "return Some(inputs) when a http response is provided with a status code of 429" in new Scope {
    shouldRetry(httpRequest -> event, Success(tooManyRequestsResponse) -> event) shouldBe Some(
      httpRequest           -> event
    )
    verify(metricsReporter, times(1)).incrementSubscriptionRetry(any, equalTo(HttpStatus(TooManyRequests)))
  }

  it should "return Some(inputs) when a http response is provided with a status in the 500 range" in new Scope {
    shouldRetry(httpRequest -> event, Success(internalServerErrorHttpResponse) -> event) shouldBe Some(
      httpRequest           -> event
    )
    verify(metricsReporter, times(1)).incrementSubscriptionRetry(any, equalTo(HttpStatus(InternalServerError)))
  }

  it should "return Some(inputs) when a RequestTimeoutException is returned" in new Scope {
    shouldRetry(
      httpRequest                                                -> event,
      Failure(RequestTimeoutException(httpRequest, "boom boom")) -> event
    ) shouldBe Some(
      httpRequest -> event
    )
    verify(metricsReporter, times(1)).incrementSubscriptionRetry(any, equalTo(ExceptionalStatus))
  }

  it should "return Some(inputs) when a RuntimeException a message containing `The http server closed the connection unexpectedly` is returned" in new Scope {
    shouldRetry(
      httpRequest                                                                                     -> event,
      Failure(new RuntimeException("The http server closed the connection unexpectedly - boom boom")) -> event
    ) shouldBe Some(
      httpRequest -> event
    )
    verify(metricsReporter, times(1)).incrementSubscriptionRetry(any, equalTo(ExceptionalStatus))
  }

  trait Scope {
    private val system: ActorSystem = ActorSystem()
    private val materializer: Materializer = Materializer(system)

    val httpRequest: HttpRequest = HttpRequest(method = POST, uri = subscriber.uri)
    val successfulHttpResponse: HttpResponse = HttpResponse(status = OK)
    val clientErrorHttpResponse: HttpResponse = HttpResponse(status = BadRequest)
    val tooManyRequestsResponse: HttpResponse = HttpResponse(status = TooManyRequests)
    val internalServerErrorHttpResponse: HttpResponse = HttpResponse(status = InternalServerError)

    val metricsReporter: MetricsReporter = mock[MetricsReporter]
    val httpRetryHandler = new HttpRetryHandlerImpl(subscriber, metricsReporter)(materializer)

    def shouldRetry: ((HttpRequest, Event), (Try[HttpResponse], Event)) => Option[(HttpRequest, Event)] =
      httpRetryHandler.shouldRetry
  }
}
