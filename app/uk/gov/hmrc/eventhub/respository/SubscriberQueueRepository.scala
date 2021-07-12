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

import play.api.Configuration
import uk.gov.hmrc.eventhub.model.{ Subscriber, SubscriberWorkItem }
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.PermanentlyFailed
import uk.gov.hmrc.mongo.workitem.{ ProcessingStatus, WorkItem, WorkItemFields, WorkItemRepository }

import java.time.{ Duration, Instant }
import scala.concurrent.{ ExecutionContext, Future }

class SubscriberQueueRepository(
  topic: String,
  subscriber: Subscriber,
  configuration: Configuration,
  mongo: MongoComponent
)(implicit ec: ExecutionContext)
    extends WorkItemRepository[SubscriberWorkItem](
      mongoComponent = mongo,
      collectionName = s"$topic-${subscriber.name}-queue",
      itemFormat = SubscriberWorkItem.subscriberWorkItemFormat,
      workItemFields = WorkItemFields.default
    ) {

  override def now(): Instant =
    Instant.now()

  override val inProgressRetryAfter: Duration =
    configuration.underlying.getDuration("queue.retryAfter")

  val numberOfRetries: Int = configuration.underlying.getInt("queue.numberOfRetries")

  val retryFailedAfter: Duration = configuration.underlying.getDuration("queue.retryFailedAfter")

  def addSubscriberWorkItems(s: Seq[SubscriberWorkItem]): Future[Seq[WorkItem[SubscriberWorkItem]]] =
    pushNewBatch(s)

  def getEvent: Future[Option[WorkItem[SubscriberWorkItem]]] =
    pullOutstanding(failedBefore = now().minus(retryFailedAfter), availableBefore = now())

  def deleteEvent(e: WorkItem[SubscriberWorkItem]): Future[Boolean] =
    completeAndDelete(e.id)

  def failed(e: WorkItem[SubscriberWorkItem]): Future[Boolean] =
    if (e.failureCount > numberOfRetries) permanentlyFailed(e) else markAs(e.id, ProcessingStatus.Failed)

  def permanentlyFailed(e: WorkItem[SubscriberWorkItem]): Future[Boolean] =
    complete(e.id, PermanentlyFailed)
}
