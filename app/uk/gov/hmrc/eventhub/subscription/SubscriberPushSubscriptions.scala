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

package uk.gov.hmrc.eventhub.subscription

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.HttpExt
import akka.stream.Attributes.LogLevels
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{Attributes, KillSwitches, Materializer, SharedKillSwitch}
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.eventhub.model.{Event, Subscriber, Topic}
import uk.gov.hmrc.eventhub.respository.SubscriberEventRepositoryFactory
import uk.gov.hmrc.eventhub.subscription.http.HttpResponseHandler.{EventSendStatus, ResponseParallelism}
import uk.gov.hmrc.eventhub.subscription.http.{HttpEventRequestBuilder, HttpResponseHandler, HttpRetryHandler}
import uk.gov.hmrc.eventhub.subscription.stream.{SubscriberEventHttpFlow, _}

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriberPushSubscriptions @Inject()(
  @Named("eventTopics") topics: Set[Topic],
  subscriberEventRepositoryFactory: SubscriberEventRepositoryFactory,
  httpExt: HttpExt,
  lifecycle: ApplicationLifecycle
)(
  implicit
  actorSystem: ActorSystem,
  materializer: Materializer,
  executionContext: ExecutionContext
) extends Logging {

  private val subscribersKillSwitch: SharedKillSwitch = KillSwitches.shared("subscribers-kill-switch")

  lifecycle.addStopHook { () =>
    Future(subscribersKillSwitch.shutdown())
  }

  logger.info(s"starting subscribers for: $topics")

  private val _: Set[NotUsed] =
    topics.flatMap { topic =>
      topic
        .subscribers
        .map { subscriber =>
          val stream = buildStream(subscriber, topic.name)
          stream
            .viaMat(subscribersKillSwitch.flow)(Keep.left)
            .to(Sink.ignore)
            .run()
        }
    }

  private def buildStream(subscriber: Subscriber, topic: String): Source[EventSendStatus, NotUsed] = {
    val repository = subscriberEventRepositoryFactory(subscriber, topic)
    val requestBuilder = (event: Event) => HttpEventRequestBuilder.build(subscriber, event) -> event
    val source = new SubscriberEventSource(repository)(actorSystem.scheduler, executionContext).source
    val responseHandler = new HttpResponseHandler(repository).handle(_)
    val httpFlow = new SubscriberEventHttpFlow(subscriber, HttpRetryHandler, httpExt).flow

    source
      .map(requestBuilder)
      .via(httpFlow)
      .mapAsync(parallelism = ResponseParallelism)(responseHandler)
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
