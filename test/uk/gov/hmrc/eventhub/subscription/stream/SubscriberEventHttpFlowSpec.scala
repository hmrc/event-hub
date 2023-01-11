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

package uk.gov.hmrc.eventhub.subscription.stream

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import akka.stream.scaladsl.{Sink, Source}
import org.mockito.IdiomaticMockito
import org.mockito.IdiomaticMockitoBase.Times
import org.mockito.MockitoSugar.when
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eventhub.config.Subscriber
import uk.gov.hmrc.eventhub.config.TestModels._
import uk.gov.hmrc.eventhub.model.TestModels._
import uk.gov.hmrc.eventhub.subscription.http.{HttpClient, HttpEventRequestBuilder, HttpRetryHandler}
import org.mockito.ArgumentMatchersSugar.*

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try

class SubscriberEventHttpFlowSpec
    extends AnyFlatSpec with Matchers with IdiomaticMockito with ScalaFutures with IntegrationPatience {

  behavior of "SubscriberEventHttpFlow.flow"

  it should "handle a successful response" in new Scope {
    when(httpClient.singleRequest(httpRequest)).thenAnswer(Future.successful(httpResponse))
    when(httpRetryHandler.shouldRetry(*, *)).thenAnswer(None)

    Source
      .single(httpRequest -> event)
      .via(subscriberEventHttpFlow(subscriber).flow)
      .runWith(Sink.head)
      .futureValue shouldBe SubscriberEventHttpResponse(Try(httpResponse), event, subscriber)

    httpRetryHandler.shouldRetry(*, *) wasCalled once
  }

  it should "handle retry calling the function returned from `httpRetryHandler.shouldRetry` * `subscriber.maxRetries`" in new Scope {
    when(httpClient.singleRequest(httpRequest)).thenAnswer(Future.successful(httpResponse))
    when(httpRetryHandler.shouldRetry(*, *))
      .thenAnswer(Some(httpRequest, event))
      .andThenAnswer(Some(httpRequest, event))

    Source
      .single(httpRequest -> event)
      .via(subscriberEventHttpFlow(subscriber).flow)
      .runWith(Sink.head)
      .futureValue shouldBe SubscriberEventHttpResponse(Try(httpResponse), event, subscriber)

    threeSeconds {
      httpRetryHandler.shouldRetry(*, *) wasCalled Times(subscriber.maxRetries)
    }
  }

  it should "handle a failure response" in new Scope {
    when(httpClient.singleRequest(httpRequest)).thenAnswer(Future.failed(new IllegalStateException("boom")))
    when(httpRetryHandler.shouldRetry(*, *)).thenAnswer(None)

    Source
      .single(httpRequest -> event)
      .via(subscriberEventHttpFlow(subscriber).flow)
      .runWith(Sink.head)
      .futureValue
      .response
      .failed
      .get
      .getMessage shouldBe "boom"

    httpRetryHandler.shouldRetry(*, *) wasCalled once
  }

  trait Scope {
    private val system: ActorSystem = ActorSystem("SubscriberEventHttpFlowSpec")
    implicit val materializer: Materializer = Materializer(system)

    val httpRetryHandler: HttpRetryHandler = mock[HttpRetryHandler]
    val httpClient: HttpClient = mock[HttpClient]

    val httpRequest: HttpRequest = HttpEventRequestBuilder.build(subscriber, event)
    val httpResponse: HttpResponse = HttpResponse()

    def subscriberEventHttpFlow(subscriber: Subscriber): SubscriberEventHttpFlow = new SubscriberEventHttpFlow(
      subscriber = subscriber,
      httpRetryHandler = httpRetryHandler,
      httpClient = httpClient
    )(materializer, scala.concurrent.ExecutionContext.global)

    def threeSeconds[T](fun: => T): T = eventually(timeout(3.seconds), interval(300.milliseconds))(fun)
  }
}
