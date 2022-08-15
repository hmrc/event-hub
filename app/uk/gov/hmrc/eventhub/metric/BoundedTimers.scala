/*
 * Copyright 2022 HM Revenue & Customs
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

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import uk.gov.hmrc.eventhub.metric.AkkaTimers.{Start, Stop}
import uk.gov.hmrc.eventhub.metric.Timers.{CompletedTimer, RunningTimer}

import scala.concurrent.Future
import scala.concurrent.duration._

class BoundedTimers(
  clock: Clock,
  maxTimers: Int
)(implicit actorSystem: ActorSystem)
    extends Timers {
  private implicit val timeout: Timeout = Timeout(3.seconds)
  private val timers: ActorRef = AkkaTimers.apply(maxTimers, actorSystem)

  override def startTimer(metricName: String): Future[RunningTimer] =
    (timers ? Start(metricName, clock.currentTime)).mapTo[RunningTimer]

  override def stopTimer(metricName: String): Future[Option[CompletedTimer]] =
    (timers ? Stop(metricName, clock.currentTime)).mapTo[Option[CompletedTimer]]
}
