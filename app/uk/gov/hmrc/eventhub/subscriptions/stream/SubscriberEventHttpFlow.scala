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

package uk.gov.hmrc.eventhub.subscriptions.stream

import akka.NotUsed
import akka.http.scaladsl.model._
import akka.http.scaladsl.{Http, HttpExt}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, RetryFlow}
import uk.gov.hmrc.eventhub.model.{Event, Subscriber}
import uk.gov.hmrc.eventhub.subscriptions.http.{HttpEventRequestBuilder, HttpRetryHandler}
import uk.gov.hmrc.eventhub.subscriptions.stream.SubscriberEventHttpFlow.{RandomFactor, SubscriberEventHttpResponse}

import scala.util.Try

trait SubscriberEventHttpFlow {
  def flow: Flow[Event, SubscriberEventHttpResponse, NotUsed]
}

class SubscriberEventHttpFlowImpl(
  subscriber: Subscriber,
  httpRetryHandler: HttpRetryHandler,
  httpEventRequestBuilder: HttpEventRequestBuilder,
  httpExt: HttpExt
)(implicit materializer: Materializer) extends SubscriberEventHttpFlow {

  private val httpFlow = httpExt.cachedHostConnectionPool[Event](
    subscriber.uri.authority.host.toString(),
    subscriber.uri.authority.port
  )

  private val requestBuilder = httpEventRequestBuilder.build(subscriber, _)

  def flow: Flow[Event, SubscriberEventHttpResponse, NotUsed] = {
    val retryHttpFlow: Flow[(HttpRequest, Event), (Try[HttpResponse], Event), Http.HostConnectionPool] =
      RetryFlow.withBackoff(
        minBackoff = subscriber.minBackOff,
        maxBackoff = subscriber.maxBackOff,
        randomFactor = RandomFactor,
        maxRetries = subscriber.maxRetries,
        flow = httpFlow
      )(httpRetryHandler.shouldRetry())

    Flow[Event]
      .map(event => requestBuilder(event) -> event)
      .throttle(subscriber.elements, subscriber.per)
      .via(retryHttpFlow)
      .map { case(response, event) =>
        response.map(_.entity.discardBytes())
        SubscriberEventHttpResponse(response, event, subscriber)
      }
  }
}

object SubscriberEventHttpFlow {
  case class SubscriberEventHttpResponse(response: Try[HttpResponse], event: Event, subscriber: Subscriber)

  val RandomFactor = 0.2
}
