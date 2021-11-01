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

import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import play.api.Configuration
import uk.gov.hmrc.eventhub.config.{Subscriber, TopicName}
import uk.gov.hmrc.eventhub.model.Event
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.PermanentlyFailed
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem, WorkItemFields, WorkItemRepository}
import uk.gov.hmrc.mongo.{MongoComponent, MongoUtils}

import java.time.{Duration, Instant}
import scala.concurrent.{ExecutionContext, Future}

class SubscriberQueueRepository(
  topicName: TopicName,
  subscriber: Subscriber,
  configuration: Configuration,
  mongo: MongoComponent
)(implicit ec: ExecutionContext)
    extends WorkItemRepository[Event](
      mongoComponent = mongo,
      collectionName = s"${topicName.name}_${subscriber.name}_queue",
      itemFormat = Event.eventFormat,
      workItemFields = WorkItemFields.default,
      replaceIndexes = false
    ) {

  override def now(): Instant =
    Instant.now()

  lazy val augmentedIndexes: Seq[IndexModel] = Seq(
    IndexModel(Indexes.ascending("item.eventId"), IndexOptions().background(true))
  ) ++ indexes

  override def ensureIndexes: Future[Seq[String]] =
    MongoUtils.ensureIndexes(collection, augmentedIndexes, true)

  override val inProgressRetryAfter: Duration =
    configuration.underlying.getDuration("queue.retryAfter")

  val numberOfRetries: Int = configuration.underlying.getInt("queue.numberOfRetries")

  val retryFailedAfter: Duration = configuration.underlying.getDuration("queue.retryFailedAfter")

  def getEvent: Future[Option[WorkItem[Event]]] =
    pullOutstanding(failedBefore = now().minus(retryFailedAfter), availableBefore = now())

  def failed(e: WorkItem[Event]): Future[Boolean] =
    if (e.failureCount > numberOfRetries) permanentlyFailed(e) else markAs(e.id, ProcessingStatus.Failed)

  def permanentlyFailed(e: WorkItem[Event]): Future[Boolean] =
    complete(e.id, PermanentlyFailed)

  def findAsWorkItem(event: Event): Future[Option[WorkItem[Event]]] =
    collection.find(equal("item.eventId", event.eventId.toString)).headOption()
}
