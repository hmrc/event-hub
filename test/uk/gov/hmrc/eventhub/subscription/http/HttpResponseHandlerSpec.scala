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

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import cats.syntax.option._
import org.mockito.IdiomaticMockito
import org.mockito.MockitoSugar.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.eventhub.model.TestModels
import uk.gov.hmrc.eventhub.repository.SubscriberEventRepository
import uk.gov.hmrc.eventhub.subscription.http.HttpResponseHandler.{EventSendStatus, Failed, Sent}
import uk.gov.hmrc.eventhub.subscription.stream.SubscriberEventHttpResponse

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class HttpResponseHandlerSpec extends AnyFlatSpec with Matchers with IdiomaticMockito with ScalaFutures {

  behavior of "HttpResponseHandler.handle"

  it should "mark an event as sent when the response is successful and the status code is a success" in new Scope {
    when(subscriberEventRepository.sent(event)) thenReturn Future.successful(true.some)
    handle(successfulResponse) mustBe EventSendStatus(event, subscriber, Sent)
  }

  it should "mark an event as failed when the response is successful and the status code is a client error" in new Scope {
    when(subscriberEventRepository.failed(event)) thenReturn Future.successful(true.some)
    handle(clientErrorResponse) mustBe EventSendStatus(event, subscriber, Failed)
  }

  it should "mark an event as failed when the response is a failure" in new Scope {
    when(subscriberEventRepository.failed(event)) thenReturn Future.successful(true.some)
    handle(failureResponse) mustBe EventSendStatus(event, subscriber, Failed)
  }

  trait Scope extends TestModels {
    val subscriberEventRepository: SubscriberEventRepository = mock[SubscriberEventRepository]
    private val handler = new HttpResponseHandler(subscriberEventRepository)

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

    val failureResponse: SubscriberEventHttpResponse = SubscriberEventHttpResponse(
      response = Failure(new IllegalStateException("boom")),
      event = event,
      subscriber = subscriber
    )
  }
}
