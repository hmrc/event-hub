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

package uk.gov.hmrc.eventhub.subscription.stream

import akka.NotUsed
import akka.http.scaladsl.model._
import akka.http.scaladsl.{ Http, HttpExt }
import akka.stream.Materializer
import akka.stream.scaladsl.{ Flow, RetryFlow }
import uk.gov.hmrc.eventhub.model.{ Event, Subscriber }
import uk.gov.hmrc.eventhub.subscription.http.HttpRetryHandler

import scala.util.Try

class SubscriberEventHttpFlow(
  subscriber: Subscriber,
  httpRetryHandler: HttpRetryHandler,
  httpExt: HttpExt
)(implicit materializer: Materializer) {
  private val RandomFactor = 0.2

  private val httpFlow = httpExt.cachedHostConnectionPool[Event](
    subscriber.uri.authority.host.toString(),
    subscriber.uri.authority.port
  )

  def flow: Flow[(HttpRequest, Event), SubscriberEventHttpResponse, NotUsed] = {
    val retryHttpFlow: Flow[(HttpRequest, Event), (Try[HttpResponse], Event), Http.HostConnectionPool] =
      RetryFlow.withBackoff(
        minBackoff = subscriber.minBackOff,
        maxBackoff = subscriber.maxBackOff,
        randomFactor = RandomFactor,
        maxRetries = subscriber.maxRetries,
        flow = httpFlow
      )(httpRetryHandler.shouldRetry())

    Flow[(HttpRequest, Event)]
      .throttle(subscriber.elements, subscriber.per)
      .via(retryHttpFlow)
      .map(response)
  }

  private def response(tuple: (Try[HttpResponse], Event)): SubscriberEventHttpResponse = tuple match {
    case (response, event) =>
      response.map(_.entity.discardBytes())
      SubscriberEventHttpResponse(response, event, subscriber)
  }
}