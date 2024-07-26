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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Sink
import org.mockito.Mockito.{verify, times, when, atLeastOnce, atLeast => atLeastTimes}
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Seconds, Span}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eventhub.model.Event
import uk.gov.hmrc.eventhub.model.TestModels.event
import uk.gov.hmrc.eventhub.repository.SubscriberEventRepository

import scala.concurrent.duration.*
import scala.concurrent.Future

class SubscriberEventSourceSpec extends AnyFlatSpec with Matchers with MockitoSugar with ScalaFutures {

  behavior of "SubscriberEventSource.source"

  it should "provide all the events for the downstream demand" in new Scope {
    when(subscriberEventRepository.next())
      .thenReturn(someFutureEvent)
      .thenAnswer(_ => someFutureEvent)
      .thenAnswer(_ => someFutureEvent)

    subscriberEventSource
      .source
      .take(3)
      .runWith(Sink.seq)
      .futureValue shouldBe Seq(event, event, event)

    verify(subscriberEventRepository, times(3)).next()
  }

  it should "provide an event for a single ask" in new Scope {
    when(subscriberEventRepository.next()).thenReturn(someFutureEvent)

    subscriberEventSource
      .source
      .take(1)
      .runWith(Sink.seq)
      .futureValue shouldBe Seq(event)

    verify(subscriberEventRepository, atLeastOnce()).next()
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
      verify(subscriberEventRepository, atLeastTimes(soManyTimes)).next()
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
