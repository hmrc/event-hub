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
import play.api.Logging
import uk.gov.hmrc.eventhub.model.{ Event, Subscriber }
import uk.gov.hmrc.eventhub.subscription.http.HttpRetryHandler
import uk.gov.hmrc.eventhub.subscription.stream.SubscriberEventHttpFlow.{ HttpsScheme, RandomFactor }

import scala.util.Try

class SubscriberEventHttpFlow(
  subscriber: Subscriber,
  httpRetryHandler: HttpRetryHandler,
  httpExt: HttpExt
)(implicit materializer: Materializer)
    extends Logging {

  private val httpFlow = {
    val poolCons = if (subscriber.uri.scheme == HttpsScheme) {
      logger.info(s"creating https connection pool for ${subscriber.uri.authority.host.toString()} on port: ${subscriber.uri.authority.port}")
      httpExt.cachedHostConnectionPoolHttps[Event](_: String, _: Int)
    } else {
      logger.info(s"creating http connection pool for ${subscriber.uri.authority.host.toString()} on port: ${subscriber.uri.authority.port}")
      httpExt.cachedHostConnectionPool[Event](_: String, _: Int)
    }

    poolCons(
      subscriber.uri.authority.host.toString(),
      subscriber.uri.authority.port
    )
  }

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

object SubscriberEventHttpFlow {
  val HttpsScheme = "https"
  val RandomFactor = 0.2
}
