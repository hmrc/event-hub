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
import org.apache.pekko.testkit.{ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eventhub.metric.ActorTimers.{Start, Stop}
import uk.gov.hmrc.eventhub.metric.Timers.{CompletedTimer, RunningTimer}

class ActorTimersSpec
    extends TestKit(ActorSystem("ActorTimersSpec")) with ImplicitSender with AnyFlatSpecLike with Matchers
    with BeforeAndAfterAll {

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  behavior of "ActorTimers"

  it should "return a running timer" in {
    val actorTimers = ActorTimers.apply(1, system)
    val metricName = "test"
    val millis = System.currentTimeMillis()

    actorTimers ! Start(metricName, millis)
    expectMsg(RunningTimer(millis))
  }

  it should "return a completed timer" in {
    val actorTimers = ActorTimers.apply(1, system)
    val metricName = "test"
    val start = System.currentTimeMillis()

    actorTimers ! Start(metricName, start)
    expectMsg(RunningTimer(start))

    val end = System.currentTimeMillis()

    actorTimers ! Stop(metricName, end)
    expectMsg(Some(CompletedTimer(start, end)))
  }

  it should "return none when the timer for a metric is not running" in {
    val actorTimers = ActorTimers.apply(1, system)
    val metricName = "test"
    val end = System.currentTimeMillis()

    actorTimers ! Stop(metricName, end)
    expectMsg(None)
  }

  it should "return none when the timer for a metric that was running is dropped" in {
    val actorTimers = ActorTimers.apply(1, system)
    val metricName = "test"
    val start = System.currentTimeMillis()

    actorTimers ! Start(metricName, start)
    expectMsg(RunningTimer(start))

    actorTimers ! Start("foo", start)
    expectMsg(RunningTimer(start))

    val end = System.currentTimeMillis()

    actorTimers ! Stop(metricName, end)
    expectMsg(None)
  }

  it should "fail to instance actor timers when maxTimers is not > 0" in {
    the[IllegalArgumentException] thrownBy ActorTimers.apply(
      -1,
      system
    ) should have message "requirement failed: max timers must be > 0"
  }
}
