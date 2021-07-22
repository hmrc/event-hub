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

import org.mockito.IdiomaticMockito
import org.mongodb.scala.{ MongoCollection, Observable }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eventhub.model.SubscriberWorkItem
import uk.gov.hmrc.eventhub.respository.{ SubscriberQueueRepository, WorkItemSubscriberEventRepository }
import uk.gov.hmrc.eventhub.model.TestModels.{ event, workItem }
import uk.gov.hmrc.mongo.workitem.WorkItem

import scala.concurrent.Future

class WorkItemSubscriberEventRepositorySpec extends AnyFlatSpec with Matchers with IdiomaticMockito with ScalaFutures {

  behavior of "WorkItemSubscriberEventRepository.next"

  it should "return an event when the underlying work item repo returns a new work item" in new Scope {
    someFutureWorkItem willBe returned by subscriberQueueRepository.getEvent

    workItemSubscriberEventRepository
      .next()
      .futureValue shouldBe Some(event)
  }

  it should "return None when the underlying work item repo returns None" in new Scope {
    futureNone willBe returned by subscriberQueueRepository.getEvent

    workItemSubscriberEventRepository
      .next()
      .futureValue shouldBe None
  }

  trait Scope {
    val mongoCollection: MongoCollection[WorkItem[SubscriberWorkItem]] = mock[MongoCollection[WorkItem[SubscriberWorkItem]]]
    val subscriberQueueRepository: SubscriberQueueRepository = mock[SubscriberQueueRepository]

    mongoCollection willBe returned by subscriberQueueRepository.collection

    val workItemSubscriberEventRepository: WorkItemSubscriberEventRepository = new WorkItemSubscriberEventRepository(
      subscriberQueueRepository
    )(scala.concurrent.ExecutionContext.global)

    def someFutureWorkItem: Future[Option[WorkItem[SubscriberWorkItem]]] =
      Future.successful(Some(workItem))

    def observableWorkItem: Observable[WorkItem[SubscriberWorkItem]] =
      Observable(Seq(workItem))

    def futureNone: Future[None.type] = Future.successful(None)

    def trueFuture: Future[Boolean] = Future.successful(true)
  }
}
