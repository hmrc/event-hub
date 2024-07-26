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

package uk.gov.hmrc.eventhub.model

import org.mongodb.scala.ClientSession
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.result.InsertOneResult
import org.mongodb.scala.SingleObservableFuture
import uk.gov.hmrc.eventhub.config.{Subscriber, TopicName}
import uk.gov.hmrc.eventhub.model.SubscriberRepository.subscriberWorkItem
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem, WorkItemRepository}

import java.time.Instant
import scala.concurrent.Future

class SubscriberRepository(
  val topicName: TopicName,
  val subscriber: Subscriber,
  val workItemRepository: WorkItemRepository[Event]
) {
  def insertOne(clientSession: ClientSession, event: Event): Future[InsertOneResult] =
    workItemRepository
      .collection
      .insertOne(clientSession, subscriberWorkItem(event))
      .toFuture()
}

object SubscriberRepository {
  def apply(
    topicName: TopicName,
    subscriber: Subscriber,
    workItemRepository: WorkItemRepository[Event]
  ): SubscriberRepository =
    new SubscriberRepository(topicName, subscriber, workItemRepository)

  def subscriberWorkItem(event: Event): WorkItem[Event] =
    WorkItem(
      id = new ObjectId(),
      receivedAt = Instant.now(),
      updatedAt = Instant.now(),
      availableAt = Instant.now(),
      status = ProcessingStatus.ToDo,
      failureCount = 0,
      item = event
    )
}
