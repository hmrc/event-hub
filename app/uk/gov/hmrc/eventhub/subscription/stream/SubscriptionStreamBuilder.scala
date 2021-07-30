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
import akka.actor.ActorSystem
import akka.http.scaladsl.HttpExt
import akka.stream.Attributes
import akka.stream.Attributes.LogLevels
import akka.stream.scaladsl.Source
import uk.gov.hmrc.eventhub.config.SubscriberStreamConfig
import uk.gov.hmrc.eventhub.model.{ Event, Subscriber }
import uk.gov.hmrc.eventhub.repository.SubscriberEventRepositoryFactory
import uk.gov.hmrc.eventhub.subscription.http.HttpResponseHandler.EventSendStatus
import uk.gov.hmrc.eventhub.subscription.http.{ HttpEventRequestBuilder, HttpResponseHandler, HttpRetryHandler }

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext

@Singleton
class SubscriptionStreamBuilder @Inject()(
  subscriberEventRepositoryFactory: SubscriberEventRepositoryFactory,
  subscriberStreamConfig: SubscriberStreamConfig,
  httpExt: HttpExt
)(implicit actorSystem: ActorSystem, executionContext: ExecutionContext) {

  def build(subscriber: Subscriber, topic: String): Source[EventSendStatus, NotUsed] = {
    val repository = subscriberEventRepositoryFactory(subscriber, topic)
    val source = new SubscriberEventSource(repository, subscriberStreamConfig.eventPollingInterval)(actorSystem.scheduler, executionContext).source
    val requestBuilder = (event: Event) => HttpEventRequestBuilder.build(subscriber, event) -> event
    val httpFlow = new SubscriberEventHttpFlow(subscriber, HttpRetryHandler, httpExt).flow
    val responseHandler = new HttpResponseHandler(repository).handle(_)

    source
      .map(requestBuilder)
      .via(httpFlow)
      .mapAsync(parallelism = subscriber.maxConnections)(responseHandler)
      .log(s"$topic-${subscriber.name} subscription")
      .withAttributes(
        Attributes.logLevels(
          onElement = LogLevels.Debug,
          onFinish = LogLevels.Info,
          onFailure = LogLevels.Error
        )
      )
  }
}
