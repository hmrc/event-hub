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

package uk.gov.hmrc.eventhub.metric

import akka.actor.Actor
import uk.gov.hmrc.eventhub.metric.AkkaTimers.{Start, Stop}
import uk.gov.hmrc.eventhub.metric.Timers.{CompletedTimer, RunningTimer}

import scala.collection.immutable.ListMap

object AkkaTimers {
  case class Start(metricName: String, millis: Long)
  case class Stop(metricName: String, millis: Long)
}

// Bounded ListMap of `String -> Long`, drops the oldest element when full.
class AkkaTimers(maxTimers: Int) extends Actor {
  require(maxTimers > 0, s"max timers must be > 0")
  private val timers: Map[String, Long] = ListMap.empty

  override def receive: Receive = onMessage(timers)

  private def onMessage(timers: Map[String, Long]): Receive = {
    case Start(metricName, millis) =>
      val withTimer = timers + (metricName -> millis)

      if (withTimer.size > maxTimers) {
        context.become(onMessage(withTimer.dropRight(1)))
      } else {
        context.become(onMessage(withTimer))
      }

      sender() ! RunningTimer(millis)

    case Stop(metricName, millis) =>
      val maybeCompletedTimer = timers
        .get(metricName)
        .map(start => CompletedTimer(start, millis))

      context.become(onMessage(timers - metricName))
      sender() ! maybeCompletedTimer
  }
}
