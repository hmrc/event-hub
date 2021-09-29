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

import com.mongodb.client.result.InsertOneResult
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito
import org.mongodb.scala.bson.BsonObjectId
import org.mongodb.scala.result.InsertOneResult
import org.mongodb.scala.{ClientSession, MongoCollection, SingleObservable}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eventhub.config.{Subscriber, TopicName}
import uk.gov.hmrc.eventhub.model.TestModels.{channelPreferences, event}
import uk.gov.hmrc.eventhub.model.{Event, SubscriberRepository}
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.ToDo
import uk.gov.hmrc.mongo.workitem.{WorkItem, WorkItemRepository}

class SubscriberRepositorySpec extends AnyFlatSpec with Matchers with IdiomaticMockito with ScalaFutures {

  behavior of "SubscriberRepository.insertOne"

  it should "return a mongo InsertOneResult when the underlying repo adds a new event to the event repo" in new Scope {
    mongoCollectionMock.insertOne(clientSessionMock, *[WorkItem[Event]]) returns SingleObservable(insertOneResult)
    subscriberRepository.insertOne(clientSessionMock, event).futureValue shouldBe insertOneResult
  }

  behavior of "SubscriberRepository.subscriberWorkItem"

  it should "uplift an event into it's work item counterpart" in new Scope {
    val workItem = SubscriberRepository.subscriberWorkItem(event)
    workItem.status should be(ToDo)
    workItem.failureCount should be(0)
    workItem.item should be(event)
  }

  trait Scope {
    val clientSessionMock: ClientSession = mock[ClientSession]
    val workItemRepositoryMock: WorkItemRepository[Event] = mock[WorkItemRepository[Event]]
    val mongoCollectionMock: MongoCollection[WorkItem[Event]] = mock[MongoCollection[WorkItem[Event]]]
    val topicName: TopicName = TopicName(name = "topicName")
    val subscriber: Subscriber = channelPreferences
    val subscriberRepository: SubscriberRepository =
      new SubscriberRepository(topicName, subscriber, workItemRepositoryMock)
    val insertOneResult: InsertOneResult = InsertOneResult.acknowledged(BsonObjectId())

    workItemRepositoryMock.collection returns mongoCollectionMock
  }
}
