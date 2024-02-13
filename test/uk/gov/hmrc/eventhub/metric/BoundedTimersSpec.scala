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

package uk.gov.hmrc.eventhub.metric

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.testkit.TestKit
import org.mockito.IdiomaticMockito
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eventhub.metric.Timers.{CompletedTimer, RunningTimer}

class BoundedTimersSpec
    extends TestKit(ActorSystem("BoundedTimersSpec")) with AnyFlatSpecLike with Matchers with BeforeAndAfterAll
    with ScalaFutures with IdiomaticMockito {

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  behavior of "BoundedTimers"

  it should "return a running timer for a given metric name" in new Scope {
    clock.currentTime returns startTime
    boundedTimers
      .startTimer("foo")
      .futureValue shouldBe RunningTimer(startTime)
  }

  it should "return a completed timer for a given metric name" in new Scope {
    clock.currentTime returns startTime
    boundedTimers
      .startTimer("foo")
      .futureValue shouldBe RunningTimer(startTime)

    clock.currentTime returns endTime
    boundedTimers
      .stopTimer("foo")
      .futureValue shouldBe Some(CompletedTimer(startTime, endTime))
  }

  trait Scope {
    val startTime: Long = System.currentTimeMillis()
    val endTime: Long = startTime + 1000
    val clock: Clock = mock[Clock]
    val boundedTimers = new BoundedTimers(clock, 1)(system)
  }
}
