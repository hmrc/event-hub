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

package uk.gov.hmrc.eventhub.subscription

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.Status.CREATED
import uk.gov.hmrc.eventhub.model.Event
import uk.gov.hmrc.eventhub.subscription.Setup.scope
import uk.gov.hmrc.eventhub.subscription.TestModels.Http.requestPatternBuilderOps
import uk.gov.hmrc.eventhub.subscription.TestModels.{ Events, Subscriptions }
import scala.concurrent.ExecutionContext.Implicits.global
import Arbitraries._

import scala.concurrent.Future

class SubscriberPushSubscriptionsISpec extends AnyFlatSpec with ISpec with ScalaCheckDrivenPropertyChecks {

  behavior of "SubscriberPushSubscriptions"

  it should "push an event to a registered subscriber" in scope(Subscriptions.channelPreferencesBouncedEmails) {
    setup =>
      val response = setup
        .postToTopic(Subscriptions.BoundedEmailsTopic, Events.event)
        .futureValue

      response.status mustBe CREATED

      val channelPreferencesServer = setup
        .subscriberServer(Subscriptions.ChannelPreferencesBounced)
        .value

      oneSecond {
        channelPreferencesServer
          .verify(postRequestedFor(urlEqualTo(Subscriptions.ChannelPreferencesBouncedPath)).withEventJson(Events.event))
      }
  }

  it should "push an event to registered subscribers" in scope(Subscriptions.bouncedEmails) { setup =>
    val response = setup
      .postToTopic(Subscriptions.BoundedEmailsTopic, Events.event)
      .futureValue

    response.status mustBe CREATED

    val channelPreferencesServer = setup
      .subscriberServer(Subscriptions.ChannelPreferencesBounced)
      .value

    val anotherPartyServer = setup
      .subscriberServer(Subscriptions.AnotherPartyBounced)
      .value

    oneSecond {
      channelPreferencesServer
        .verify(postRequestedFor(urlEqualTo(Subscriptions.ChannelPreferencesBouncedPath)).withEventJson(Events.event))

      anotherPartyServer
        .verify(postRequestedFor(urlEqualTo(Subscriptions.AnotherPartyBouncedPath)).withEventJson(Events.event))
    }
  }

  it should "push events to all registered subscribers" in scope(Subscriptions.bouncedEmails) { setup =>
    forAll { eventList: List[Event] =>
      val responses = Future
        .sequence(eventList.map { event =>
          setup
            .postToTopic(Subscriptions.BoundedEmailsTopic, event)
        })
        .futureValue

      responses.foreach(_.status mustBe CREATED)

      val channelPreferencesServer = setup
        .subscriberServer(Subscriptions.ChannelPreferencesBounced)
        .value

      val anotherPartyServer = setup
        .subscriberServer(Subscriptions.AnotherPartyBounced)
        .value

      fiveMinutes {
        eventList.foreach { event =>
          channelPreferencesServer
            .verify(postRequestedFor(urlEqualTo(Subscriptions.ChannelPreferencesBouncedPath)).withEventJson(event))

          anotherPartyServer
            .verify(postRequestedFor(urlEqualTo(Subscriptions.AnotherPartyBouncedPath)).withEventJson(event))
        }
      }
    }
  }
}
