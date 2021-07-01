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
import akka.actor.Scheduler
import akka.pattern.Patterns.after
import akka.stream.scaladsl.Source
import play.api.Logging
import uk.gov.hmrc.eventhub.model.Event
import uk.gov.hmrc.eventhub.respository.SubscriberEventRepository

import java.util.concurrent.Callable
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

trait SubscriberEventSource {
  def source: Source[Event, NotUsed]
}

/**
  * Polls for work items, emits when there is an available work item and downstream demand
  */
class PullSubscriberEventSource(
  subscriberEventRepository: SubscriberEventRepository
)(
  implicit
  scheduler: Scheduler,
  executionContext: ExecutionContext)
    extends SubscriberEventSource with Logging {

  /**
    * TODO make `after` delay configurable
    */
  private def onPull: Unit => Future[Option[(Unit, Event)]] = { _ =>
    logger.info(s"polling subscriber repository...")
    subscriberEventRepository
      .next()
      .flatMap(pullLogic)
  }

  private def pullLogic(readResult: Option[Event]): Future[Option[(Unit, Event)]] = readResult match {
    case None =>
      logger.info("no dice, retrying in 500 millis")
      after(500.millis, scheduler, executionContext, onPullCallable)
    case Some(event) =>
      logger.info(s"found event $event")
      Future.successful(Some(() -> event))
  }

  private def onPullCallable: Callable[Future[Option[(Unit, Event)]]] =
    new Callable[Future[Option[(Unit, Event)]]] {
      override def call: Future[Option[(Unit, Event)]] = onPull(())
    }

  def source: Source[Event, NotUsed] =
    Source.unfoldAsync(())(onPull)
}
