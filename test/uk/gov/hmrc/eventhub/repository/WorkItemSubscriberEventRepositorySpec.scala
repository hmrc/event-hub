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

import ch.qos.logback.classic.Level
import org.mockito.ArgumentMatchersSugar.*
import org.mockito.IdiomaticMockito
import org.mongodb.scala.Observable
import org.mongodb.scala.bson.ObjectId
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eventhub.helpers.LogCapturing
import uk.gov.hmrc.eventhub.model.Event
import uk.gov.hmrc.eventhub.model.TestModels.{event, workItem}
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.PermanentlyFailed
import uk.gov.hmrc.mongo.workitem.WorkItem

import scala.concurrent.Future

class WorkItemSubscriberEventRepositorySpec
    extends AnyFlatSpec with Matchers with IdiomaticMockito with ScalaFutures with LogCapturing {

  behavior of "WorkItemSubscriberEventRepository.next"

  it should "return an event when the underlying work item repo returns a new work item" in new Scope {
    someFutureWorkItem willBe returned by subscriberQueueRepository.getEvent

    workItemSubscriberEventRepository.next().futureValue should be(Some(event))
  }

  it should "return None when the underlying work item repo returns None" in new Scope {
    futureNone willBe returned by subscriberQueueRepository.getEvent

    workItemSubscriberEventRepository.next().futureValue should be(None)
  }

  it should "attempt to mark the work item as permanently failed when the event document json could not be deserialized and call next on success" in new Scope {
    val eventId = "61360df8bcf93f52c00f3dc8"
    val failedId = new ObjectId(eventId)
    val deserError = new RuntimeException(
      """Failed to parse json as uk.gov.hmrc.mongo.workitem.WorkItem '{"_id":{"$oid":"61360df8bcf93f52c00f3dc8"},"""
    )

    subscriberQueueRepository.getEvent returns (Future.failed(deserError), futureNone)
    subscriberQueueRepository.complete(failedId, PermanentlyFailed) returns Future.successful(true)

    workItemSubscriberEventRepository
      .next()
      .futureValue should be(None)

    subscriberQueueRepository.complete(failedId, PermanentlyFailed) wasCalled once
    subscriberQueueRepository.getEvent wasCalled twice
  }

  it should "attempt to mark the work item as permanently failed when the event document json could not be deserialized " +
    "and rethrow when unsuccessful" in new Scope {
      val eventId = "61360df8bcf93f52c00f3dc8"
      val failedId = new ObjectId(eventId)
      val deserError = new RuntimeException(
        """Failed to parse json as uk.gov.hmrc.mongo.workitem.WorkItem '{"_id":{"$oid":"61360df8bcf93f52c00f3dc8"},"""
      )

      subscriberQueueRepository.getEvent returns (Future.failed(deserError), futureNone)
      subscriberQueueRepository.complete(failedId, PermanentlyFailed) returns Future.successful(false)

      workItemSubscriberEventRepository
        .next()
        .failed
        .futureValue
        .getMessage should be(deserError.getMessage)

      subscriberQueueRepository.complete(failedId, PermanentlyFailed) wasCalled once
      subscriberQueueRepository.getEvent wasCalled once
    }

  behavior of "WorkItemSubscriberEventRepository.failed"

  it should "find an work item containing an event and mark it as failed" in new Scope {
    someFutureWorkItem willBe returned by subscriberQueueRepository.findAsWorkItem(*[Event])
    futureTrue willBe returned by subscriberQueueRepository.failed(workItem)

    withCaptureOfLoggingFrom[WorkItemSubscriberEventRepository] { logEvents =>
      workItemSubscriberEventRepository.failed(event).futureValue should be(Some(true))
      logEvents.head.getLevel should be(Level.DEBUG)
      logEvents.head.getMessage.contains("marking") should be(true)
      logEvents.head.getMessage.contains("as failed: true") should be(true)
    }
  }

  behavior of "WorkItemSubscriberEventRepository.sent"

  it should "find an work item containing an event and mark it as completed and delete" in new Scope {
    someFutureWorkItem willBe returned by subscriberQueueRepository.findAsWorkItem(*[Event])
    futureTrue willBe returned by subscriberQueueRepository.completeAndDelete(*[ObjectId])

    withCaptureOfLoggingFrom[WorkItemSubscriberEventRepository] { logEvents =>
      workItemSubscriberEventRepository.sent(event).futureValue should be(Some(true))
      logEvents.head.getLevel should be(Level.DEBUG)
      logEvents.head.getMessage.contains("marking") should be(true)
      logEvents.head.getMessage.contains("as sent: true") should be(true)
    }
  }

  behavior of "WorkItemSubscriberEventRepository.remove"

  it should "find an work item containing an event and mark it as permanently failed" in new Scope {
    someFutureWorkItem willBe returned by subscriberQueueRepository.findAsWorkItem(*[Event])
    futureTrue willBe returned by subscriberQueueRepository.permanentlyFailed(workItem)

    withCaptureOfLoggingFrom[WorkItemSubscriberEventRepository] { logEvents =>
      workItemSubscriberEventRepository.remove(event).futureValue should be(Some(true))
      logEvents.head.getLevel should be(Level.DEBUG)
      logEvents.head.getMessage.contains("removing") should be(true)
      logEvents.head.getMessage.contains(": true") should be(true)
    }
  }

  trait Scope {
    val subscriberQueueRepository: SubscriberQueueRepository = mock[SubscriberQueueRepository]

    val workItemSubscriberEventRepository: WorkItemSubscriberEventRepository = new WorkItemSubscriberEventRepository(
      subscriberQueueRepository
    )(scala.concurrent.ExecutionContext.global)

    def someFutureWorkItem: Future[Option[WorkItem[Event]]] =
      Future.successful(Some(workItem))

    def observableWorkItem: Observable[WorkItem[Event]] =
      Observable(Seq(workItem))

    def futureNone: Future[None.type] = Future.successful(None)

    def futureTrue: Future[Boolean] = Future.successful(true)
  }
}
