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

package uk.gov.hmrc.eventhub.config

import akka.stream.RestartSettings

import scala.concurrent.duration.FiniteDuration

case class SubscriberStreamBackoffConfig(
  minBackOff: FiniteDuration,
  maxBackOff: FiniteDuration
)

object SubscriberStreamBackoffConfig {
  val randomFactor = 0.2

  implicit class Ops(val subscriberStreamBackoffConfig: SubscriberStreamBackoffConfig) extends AnyVal {
    def asRestartSettings: RestartSettings = RestartSettings(
      subscriberStreamBackoffConfig.minBackOff,
      subscriberStreamBackoffConfig.maxBackOff,
      randomFactor
    )
  }
}
