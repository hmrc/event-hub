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

package uk.gov.hmrc.eventhub.subscription

import akka.NotUsed
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.{KillSwitches, Materializer, SharedKillSwitch}
import play.api.Logging
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.eventhub.config.Topic
import uk.gov.hmrc.eventhub.subscription.stream._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriberPushSubscriptions @Inject() (
  topics: Set[Topic],
  subscriptionStreamBuilder: SubscriptionStreamBuilder,
  lifecycle: ApplicationLifecycle
)(implicit
  materializer: Materializer,
  executionContext: ExecutionContext
) extends Logging {

  private val subscribersKillSwitch: SharedKillSwitch = KillSwitches.shared("subscribers-kill-switch")

  lifecycle.addStopHook { () =>
    Future(subscribersKillSwitch.shutdown())
  }

  logger.info(s"starting subscribers for: $topics")

  private val _: Set[NotUsed] =
    topics
      .flatMap { topic =>
        topic
          .subscribers
          .map { subscriber =>
            val stream = subscriptionStreamBuilder.build(subscriber, topic.name)
            stream
              .viaMat(subscribersKillSwitch.flow)(Keep.left)
              .to(Sink.ignore)
              .run()
          }
      }
}
