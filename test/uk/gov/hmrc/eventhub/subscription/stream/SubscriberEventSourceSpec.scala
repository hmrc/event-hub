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

package uk.gov.hmrc.eventhub.subscription.stream

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import org.mockito.IdiomaticMockito
import org.mockito.IdiomaticMockitoBase.AtLeast
import org.mockito.MockitoSugar.when
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import uk.gov.hmrc.eventhub.model.Event
import uk.gov.hmrc.eventhub.model.TestModels.event
import uk.gov.hmrc.eventhub.repository.SubscriberEventRepository
import scala.concurrent.duration._

import scala.concurrent.Future

class SubscriberEventSourceSpec extends AnyFlatSpec with Matchers with IdiomaticMockito with ScalaFutures {

  behavior of "SubscriberEventSource.source"

  it should "provide all the events for the downstream demand" in new Scope {
    when(subscriberEventRepository.next())
      .thenReturn(someFutureEvent)
      .andThenAnswer(someFutureEvent)
      .andThenAnswer(someFutureEvent)

    subscriberEventSource
      .source
      .take(3)
      .runWith(Sink.seq)
      .futureValue shouldBe Seq(event, event, event)

    subscriberEventRepository.next() wasCalled threeTimes
  }

  it should "provide an event for a single ask" in new Scope {
    when(subscriberEventRepository.next()).thenReturn(someFutureEvent)

    subscriberEventSource
      .source
      .take(1)
      .runWith(Sink.seq)
      .futureValue shouldBe Seq(event)

    subscriberEventRepository.next() wasCalled once
  }

  it should "continuously poll the repository for events when there is unsatisfied demand" in new Scope {
    when(subscriberEventRepository.next()).thenReturn(Future.successful(None))

    subscriberEventSource
      .source
      .take(1)
      .runWith(Sink.seq)

    val timeInSeconds = 2

    eventually(timeout = Timeout(Span(timeInSeconds, Seconds))) {
      val soManyTimes: Int = ((timeInSeconds - 1) * 1000) / pollingInterval.toMillis.toInt
      subscriberEventRepository.next() wasCalled AtLeast(soManyTimes)
    }
  }

  trait Scope {
    val subscriberEventRepository: SubscriberEventRepository = mock[SubscriberEventRepository]
    val pollingInterval: FiniteDuration = 500.millis

    val system: ActorSystem = ActorSystem()
    private val scheduler = system.scheduler
    implicit val materializer: Materializer = Materializer(system)

    val subscriberEventSource = new SubscriberEventSource(
      subscriberEventRepository,
      pollingInterval
    )(scheduler, scala.concurrent.ExecutionContext.global)

    def someFutureEvent: Future[Some[Event]] = Future.successful(Some(event))
  }
}
