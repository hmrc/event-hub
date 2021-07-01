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

package uk.gov.hmrc.eventhub.subscriptions

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{KillSwitches, Materializer, SharedKillSwitch}
import play.api.{Configuration, Logging}
import uk.gov.hmrc.eventhub.model.{Subscriber, Topic}
import uk.gov.hmrc.eventhub.respository.{SubscriberEventRepository, SubscriberQueueRepository, WorkItemSubscriberEventRepository}
import uk.gov.hmrc.eventhub.subscriptions.http.{EventMarkingHttpResponseHandler, HttpEventRequestBuilder, HttpResponseHandler, HttpRetryHandler}
import uk.gov.hmrc.eventhub.subscriptions.stream._
import uk.gov.hmrc.mongo.MongoComponent

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class SubscriberPushSubscriptions @Inject()(
  @Named("eventTopics") topics: Map[String, Topic],
  configuration: Configuration,
  mongoComponent: MongoComponent
)(
  implicit
  actorSystem: ActorSystem,
  materializer: Materializer,
  executionContext: ExecutionContext
) extends Logging {

  /**
   * TODO decide on what aggregation if any we should be providing along side the kill switch
   * Do we need a kill switch per subscriber stream?
   */
  val subscribersKillSwitch: SharedKillSwitch = KillSwitches.shared("subscribers-kill-switch")

  logger.info(s"starting subscribers for: $topics")

  private val _: List[NotUsed] = topics.toList.flatMap { case (_, topic) =>
    topic
      .subscribers
      .map { subscriber =>
        val stream = buildSubscriberStream(subscriber, topic)
        stream
          .viaMat(subscribersKillSwitch.flow)(Keep.left)
          .to(Sink.ignore)
          .run()
      }
  }

  private def buildSubscriberStream(subscriber: Subscriber, topic: Topic): Source[HttpResponseHandler.EventSendStatus, NotUsed] = {
    val subscriberQueueRepository: SubscriberQueueRepository = new SubscriberQueueRepository(
      topic.name,
      subscriber,
      configuration,
      mongoComponent
    )
    val subscriberEventRepository: SubscriberEventRepository = new WorkItemSubscriberEventRepository(subscriberQueueRepository)
    val subscriberEventSource: SubscriberEventSource = new PullSubscriberEventSource(subscriberEventRepository)(actorSystem.scheduler, executionContext)
    val subscriberEventHttpFlow: SubscriberEventHttpFlow = new SubscriberEventHttpFlowImpl(
      subscriber,
      HttpRetryHandler,
      HttpEventRequestBuilder,
      Http()
    )
    val httpResponseHandler: HttpResponseHandler = new EventMarkingHttpResponseHandler(subscriberEventRepository)

    PushSubscription.subscriptionStream(
      subscriberEventSource,
      subscriberEventHttpFlow,
      httpResponseHandler
    )
  }
}
