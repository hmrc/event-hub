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

import org.apache.pekko.actor.{Actor, ActorRef, ActorSystem, Props}
import play.api.Logging
import uk.gov.hmrc.eventhub.metric.ActorTimers.{Start, Stop}
import uk.gov.hmrc.eventhub.metric.Timers.{CompletedTimer, RunningTimer}

import scala.collection.mutable

object ActorTimers {
  def apply(maxTimers: Int, actorSystem: ActorSystem): ActorRef = {
    require(maxTimers > 0, s"max timers must be > 0")
    actorSystem.actorOf(Props(classOf[ActorTimers], maxTimers))
  }

  case class Start(metricName: String, millis: Long)
  case class Stop(metricName: String, millis: Long)
}

// Bounded ListMap of `String -> Long`, drops the most recent element when full.
private class ActorTimers(maxTimers: Int) extends Actor with Logging {
  private val timers: mutable.Map[String, Long] = mutable.LinkedHashMap.empty
  private var newestEntry: String = "newest"

  override def receive: Receive = {
    case Start(metricName, millis) =>
      if (timers.size >= maxTimers) timers -= newestEntry

      timers += (metricName -> millis)
      newestEntry = metricName
      sender() ! RunningTimer(millis)

    case Stop(metricName, millis) =>
      val maybeCompletedTimer = timers
        .get(metricName)
        .map(start => CompletedTimer(start, millis))

      timers -= metricName
      sender() ! maybeCompletedTimer
  }
}
