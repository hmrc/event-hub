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

import org.apache.pekko.http.scaladsl.model.{HttpResponse, StatusCodes}
import cats.syntax.option.*
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.mockito.ArgumentMatchers.*
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eventhub.config.TestModels.*
import uk.gov.hmrc.eventhub.metric.MetricsReporter
import uk.gov.hmrc.eventhub.model.TestModels.*
import uk.gov.hmrc.eventhub.repository.SubscriberEventRepository
import uk.gov.hmrc.eventhub.subscription.http.HttpResponseHandler.{EventSendStatus, Failed, Removed, Sent}
import uk.gov.hmrc.eventhub.subscription.stream.SubscriberEventHttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class HttpResponseHandlerSpec extends AnyFlatSpec with Matchers with MockitoSugar with ScalaFutures {

  behavior of "HttpResponseHandler.handle"

  it should "mark an event as sent when the response is successful and the status code is a success" in new Scope {
    when(subscriberEventRepository.sent(event)) thenReturn Future.successful(true.some)
    handle(successfulResponse) mustBe EventSendStatus(event, subscriber, Sent)
    verify(metricsReporter, times(1)).stopSubscriptionPublishTimer(any, any)
  }

  it should "remove an event when the response is successful and the status code is 500 error" in new Scope {
    when(subscriberEventRepository.failed(event)) thenReturn Future.successful(true.some)
    handle(internalServerErrorResponse) mustBe EventSendStatus(event, subscriber, Failed)
    verify(metricsReporter, times(1)).incrementSubscriptionFailure(any)
    verify(metricsReporter, times(1)).stopSubscriptionPublishTimer(any, any)
  }

  it should "mark an event as failed when the response is successful and the status code is 429" in new Scope {
    when(subscriberEventRepository.failed(event)) thenReturn Future.successful(true.some)
    handle(tooManyRequests) mustBe EventSendStatus(event, subscriber, Failed)
    verify(metricsReporter, times(1)).incrementSubscriptionFailure(any)
    verify(metricsReporter, times(1)).stopSubscriptionPublishTimer(any, any)
  }

  it should "mark an event as failed when the response is a failure" in new Scope {
    when(subscriberEventRepository.failed(event)) thenReturn Future.successful(true.some)
    handle(failureResponse) mustBe EventSendStatus(event, subscriber, Failed)
    verify(metricsReporter, times(1)).incrementSubscriptionFailure(any)
    verify(metricsReporter, times(1)).stopSubscriptionPublishTimer(any, any)
  }

  it should "remove an event when the response is successful and the status code is a client error" in new Scope {
    when(subscriberEventRepository.remove(event)) thenReturn Future.successful(true.some)
    handle(clientErrorResponse) mustBe EventSendStatus(event, subscriber, Removed)
    verify(metricsReporter, times(1)).incrementSubscriptionPermanentFailure(any)
    verify(metricsReporter, times(1)).stopSubscriptionPublishTimer(any, any)
  }

  it should "remove an event when the response is successful and the status code is a redirection" in new Scope {
    when(subscriberEventRepository.remove(event)) thenReturn Future.successful(true.some)
    handle(redirectionResponse) mustBe EventSendStatus(event, subscriber, Removed)
    verify(metricsReporter, times(1)).incrementSubscriptionPermanentFailure(any)
    verify(metricsReporter, times(1)).stopSubscriptionPublishTimer(any, any)
  }

  trait Scope {
    val subscriberEventRepository: SubscriberEventRepository = mock[SubscriberEventRepository]
    val metricsReporter: MetricsReporter = mock[MetricsReporter]
    private val handler = new HttpResponseHandler(subscriberEventRepository, metricsReporter)

    def handle(subscriberEventHttpResponse: SubscriberEventHttpResponse): EventSendStatus =
      handler.handle(subscriberEventHttpResponse).futureValue

    val successfulResponse: SubscriberEventHttpResponse = SubscriberEventHttpResponse(
      response = Success(HttpResponse()),
      event = event,
      subscriber = subscriber
    )

    val clientErrorResponse: SubscriberEventHttpResponse = SubscriberEventHttpResponse(
      response = Success(HttpResponse(status = StatusCodes.BadRequest)),
      event = event,
      subscriber = subscriber
    )

    val redirectionResponse: SubscriberEventHttpResponse = SubscriberEventHttpResponse(
      response = Success(HttpResponse(status = StatusCodes.MovedPermanently)),
      event = event,
      subscriber = subscriber
    )

    val internalServerErrorResponse: SubscriberEventHttpResponse = SubscriberEventHttpResponse(
      response = Success(HttpResponse(status = StatusCodes.InternalServerError)),
      event = event,
      subscriber = subscriber
    )

    val tooManyRequests: SubscriberEventHttpResponse = SubscriberEventHttpResponse(
      response = Success(HttpResponse(status = StatusCodes.TooManyRequests)),
      event = event,
      subscriber = subscriber
    )

    val failureResponse: SubscriberEventHttpResponse = SubscriberEventHttpResponse(
      response = Failure(new IllegalStateException("boom")),
      event = event,
      subscriber = subscriber
    )
  }
}
