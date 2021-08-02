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

package uk.gov.hmrc.eventhub.repository

import cats.data.OptionT
import cats.syntax.option._
import play.api.Logging
import uk.gov.hmrc.eventhub.model.Event

import scala.concurrent.{ ExecutionContext, Future }

class WorkItemSubscriberEventRepository(
  subscriberQueueRepository: SubscriberQueueRepository
)(implicit executionContext: ExecutionContext)
    extends SubscriberEventRepository with Logging {
  override def next(): Future[Option[Event]] =
    subscriberQueueRepository.getEvent
      .map(_.map(_.item))

  override def failed(event: Event): Future[Option[Boolean]] =
    (for {
      workItem <- OptionT(subscriberQueueRepository.findAsWorkItem(event))
      result   <- OptionT(subscriberQueueRepository.failed(workItem).map(_.some))
    } yield {
      logger.info(s"marking $event as failed: $result")
      result
    }).value

  override def sent(event: Event): Future[Option[Boolean]] =
    (for {
      workItem <- OptionT(subscriberQueueRepository.findAsWorkItem(event))
      result   <- OptionT(subscriberQueueRepository.completeAndDelete(workItem.id).map(_.some))
    } yield {
      logger.info(s"marking $event as sent: $result")
      result
    }).value
}
