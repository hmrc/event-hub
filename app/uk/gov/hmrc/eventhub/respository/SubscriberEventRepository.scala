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

package uk.gov.hmrc.eventhub.respository

import cats.data.OptionT
import cats.implicits._
import org.mongodb.scala.model.Filters.equal
import play.api.Logging
import uk.gov.hmrc.eventhub.model.{Event, SubscriberWorkItem}
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem}

import scala.concurrent.{ExecutionContext, Future}

trait SubscriberEventRepository {
  def next(): Future[Option[Event]]
  def failed(event: Event): Future[Option[Boolean]]
  def sent(event: Event): Future[Option[Boolean]]
}

class WorkItemSubscriberEventRepository(
  subscriberQueueRepository: SubscriberQueueRepository
)(implicit executionContext: ExecutionContext) extends SubscriberEventRepository with Logging {
  override def next(): Future[Option[Event]] =
    subscriberQueueRepository
      .getEvent
      .map(_.map(_.item.event))

  override def failed(event: Event): Future[Option[Boolean]] = {
    (for {
      workItem <- OptionT(findAsWortItem(event))
      result <- OptionT(subscriberQueueRepository.markAs(workItem.id, ProcessingStatus.Failed).map(_.some))
    } yield {
      logger.info(s"marking $event as failed: $result")
      result
    }).value
  }

  override def sent(event: Event): Future[Option[Boolean]] = {
    (for {
      workItem <- OptionT(findAsWortItem(event))
      result <- OptionT(subscriberQueueRepository.completeAndDelete(workItem.id).map(_.some))
    } yield {
      logger.info(s"marking $event as sent: $result")
      result
    }).value
  }

  private def findAsWortItem(event: Event): Future[Option[WorkItem[SubscriberWorkItem]]] =
    subscriberQueueRepository
      .collection
      .find(equal("item.eventId", event.eventId.toString))
      .headOption()
}
