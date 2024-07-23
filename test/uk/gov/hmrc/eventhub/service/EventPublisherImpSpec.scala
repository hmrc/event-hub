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

package uk.gov.hmrc.eventhub.service

import com.mongodb.client.result.InsertOneResult
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.{doNothing, when}
import org.mongodb.scala.ClientSession
import org.mongodb.scala.bson.BsonObjectId
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eventhub.model.SubscriberRepository
import uk.gov.hmrc.eventhub.model.TestModels.event
import uk.gov.hmrc.eventhub.repository.EventRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EventPublisherImpSpec extends AnyFlatSpec with Matchers with MockitoSugar with ScalaFutures {

  behavior of "EventPublisherImpl.apply"

  it should "return a successful future unit when publishing succeeds" in new Scope {
    when(eventRepository.addEvent(clientSession, event)) thenReturn Future.successful(
      InsertOneResult.acknowledged(BsonObjectId())
    )
    when(subscriberRepository.insertOne(clientSession, event)) thenReturn Future.successful(
      InsertOneResult.acknowledged(BsonObjectId())
    )
    when(transactionHandler.commit(clientSession)) thenReturn Future.unit

    eventPublisherImpl
      .apply(event, targets)
      .futureValue shouldBe ()
  }

  it should "return a failed future when adding the initial event to the EventRepository fails" in new Scope {
    when(eventRepository.addEvent(clientSession, event)) thenReturn Future.failed(new IllegalStateException("boom"))

    eventPublisherImpl
      .apply(event, targets)
      .failed
      .futureValue
      .getMessage shouldBe "boom"
  }

  it should "return a failed future when committing the transaction fails" in new Scope {
    when(eventRepository.addEvent(clientSession, event)) thenReturn Future.successful(
      InsertOneResult.acknowledged(BsonObjectId())
    )
    when(subscriberRepository.insertOne(clientSession, event)) thenReturn Future.successful(
      InsertOneResult.acknowledged(BsonObjectId())
    )
    when(transactionHandler.commit(clientSession)) thenReturn Future.failed(new IllegalStateException("commit boom"))

    eventPublisherImpl
      .apply(event, targets)
      .failed
      .futureValue
      .getMessage shouldBe "commit boom"
  }

  trait Scope {
    val clientSession: ClientSession = mock[ClientSession]
    val eventRepository: EventRepository = mock[EventRepository]
    val transactionHandler: TransactionHandler = mock[TransactionHandler]
    val publishEventAuditor: PublishEventAuditor = mock[PublishEventAuditor]

    when(transactionHandler.startTransactionSession(any, any)) thenReturn Future.successful(clientSession)
    doNothing().when(publishEventAuditor).failed(any, any)

    val eventPublisherImpl: EventPublisherImpl = new EventPublisherImpl(
      transactionHandler,
      eventRepository,
      publishEventAuditor
    )

    val subscriberRepository: SubscriberRepository = mock[SubscriberRepository]

    val targets: Set[SubscriberRepository] = Set(
      subscriberRepository,
      subscriberRepository
    )
  }
}
