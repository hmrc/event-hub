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

package uk.gov.hmrc.eventhub.subscription.model

import akka.http.scaladsl.model.{HttpMethods, Uri}
import play.api.libs.json.Json
import uk.gov.hmrc.eventhub.config.{Subscriber, Topic}
import uk.gov.hmrc.eventhub.model.Event

import scala.concurrent.duration._
import java.time.LocalDateTime
import java.util.UUID

object TestModels {

  object Events {
    val eventJson = Json.parse(
      s"""
         |{
         |  "event": "failed",
         |  "emailAddress": "hmrc-customer@some-domain.org",
         |  "detected": "2021-04-07T09:46:29+00:00",
         |  "code": 605,
         |  "reason": "Not delivering to previously bounced address",
         |  "enrolment": "HMRC-MTD-VAT~VRN~GB123456789"
         |}
         |""".stripMargin
    )

    val event: Event = Event(
      eventId = UUID.randomUUID(),
      subject = "bounced",
      groupId = "foo bar baz",
      timestamp = LocalDateTime.now(),
      event = eventJson
    )
  }

  object Subscriptions {
    val EmailTopic = "email"
    val ChannelPreferencesBounced = "channel-preferences-bounced"
    val ChannelPreferencesBouncedPath = "/channel-preferences/process/bounce"

    val Elements = 100
    val MaxRetries = 5
    val MaxConnections = 4

    val channelPreferences: Subscriber = Subscriber(
      name = ChannelPreferencesBounced,
      uri = Uri(s"http://localhost$ChannelPreferencesBouncedPath"),
      httpMethod = HttpMethods.POST,
      elements = Elements,
      per = 3.seconds,
      maxConnections = MaxConnections,
      minBackOff = 10.millis,
      maxBackOff = 1.second,
      maxRetries = MaxRetries,
      None
    )

    val AnotherPartyBounced = "another-party-bounced"
    val AnotherPartyBouncedPath = "/another-party/process/bounce"

    val anotherParty: Subscriber = Subscriber(
      name = AnotherPartyBounced,
      uri = Uri(s"http://localhost$AnotherPartyBouncedPath"),
      httpMethod = HttpMethods.POST,
      elements = Elements,
      per = 3.seconds,
      maxConnections = MaxConnections,
      minBackOff = 100.millis,
      maxBackOff = 5.minutes,
      maxRetries = MaxRetries,
      None
    )

    val channelPreferencesBouncedEmails: Topic = Topic(
      EmailTopic,
      List(
        channelPreferences
      )
    )

    val bouncedEmails: Set[Topic] = Set(
      Topic(
        EmailTopic,
        List(
          channelPreferences,
          anotherParty
        )
      )
    )
  }
}
