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

import com.mongodb.MongoException
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.{doNothing, verify, when, atLeast as atLeastTimes}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.mongodb.scala.{ClientSession, MongoClient, SingleObservable}
import org.reactivestreams.{Publisher, Subscriber, Subscription}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eventhub.config.TestModels.publisherConfig
import uk.gov.hmrc.eventhub.config.TransactionConfiguration.{sessionOptions, transactionOptions}
import uk.gov.hmrc.mongo.MongoComponent

import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext.Implicits.global

class TransactionHandlerImpSpec extends AnyFlatSpec with Matchers with MockitoSugar with ScalaFutures {

  behavior of "TransactionHandlerImpl.startTransactionSession"

  it should "return future ClientSession when the transaction and session is initialised" in new Scope {
    transactionHandlerImpl
      .startTransactionSession(sessionOptions, transactionOptions)
      .futureValue shouldBe clientSession
  }

  it should "return future unit when the transaction succeeds" in new Scope {
    transactionHandlerImpl
      .commit(clientSession)
      .futureValue shouldBe ()
  }

  it should "return failed future when the transaction fails" in new Scope {
    override def subscriptionRequestBehavior(subscriber: Subscriber[? >: Void]): Unit =
      subscriber.onError(new IllegalStateException("boom"))

    transactionHandlerImpl
      .commit(clientSession)
      .failed
      .futureValue
      .getMessage shouldBe "boom"
  }

  it should "retry the configured amount of times before giving up and returning a TransientTransaction exception" in new Scope {
    override def subscriptionRequestBehavior(subscriber: Subscriber[? >: Void]): Unit =
      subscriber.onError {
        val exception = MongoException.fromThrowable(new IllegalStateException("boom boom"))
        exception.addLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)
        exception
      }

    transactionHandlerImpl
      .commit(clientSession)
      .failed
      .futureValue
      .getMessage shouldBe s"failed to commit transaction after ${publisherConfig.transactionRetries} retry attempts, due to: boom boom"

    verify(clientSession, atLeastTimes(6)).commitTransaction()
  }

  trait Scope {
    val clientSession: ClientSession = mock[ClientSession]
    val mongoClient: MongoClient = mock[MongoClient]
    val mongoComponent: MongoComponent = mock[MongoComponent]

    // (╯°□°)╯︵ ┻━┻
    val publisher: Publisher[Void] = mock[Publisher[Void]]
    val subscription: Subscription = mock[Subscription]
    var subscriber: Subscriber[? >: Void] = scala.compiletime.uninitialized
    // The subscription request method is called twice, reacting to the second call with onComplete() errors
    val atomicCallCounter = new AtomicInteger()
    when(publisher.subscribe(any[Subscriber[? >: Void]])).thenAnswer(new Answer[Unit] {
      override def answer(invocation: InvocationOnMock): Unit = {
        val sub: Subscriber[? >: Void] = invocation.getArgument(0, classOf[Subscriber[? >: Void]])
        subscriber = sub
        atomicCallCounter.set(0)
        sub.onSubscribe(subscription)
      }
    })

    when(subscription.request(any)).thenAnswer { _ =>
      if (atomicCallCounter.getAndAdd(1) == 0) {
        subscriptionRequestBehavior(subscriber)
      }
    }

    def subscriptionRequestBehavior(subscriber: Subscriber[? >: Void]): Unit =
      subscriber.onComplete()

    when(mongoComponent.client).thenReturn(mongoClient)
    when(mongoClient.startSession(sessionOptions)).thenReturn(SingleObservable(clientSession))
    doNothing().when(clientSession).startTransaction(transactionOptions)
    when(clientSession.commitTransaction()).thenReturn(publisher)

    val transactionHandlerImpl: TransactionHandlerImpl = new TransactionHandlerImpl(
      mongoComponent,
      publisherConfig
    )
  }
}
