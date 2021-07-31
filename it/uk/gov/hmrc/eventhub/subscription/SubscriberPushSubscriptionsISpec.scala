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

import akka.http.scaladsl.model.StatusCodes.{InternalServerError, OK}
import akka.stream.scaladsl.{Sink, Source}
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.http.Status.CREATED
import uk.gov.hmrc.eventhub.model.Event
import uk.gov.hmrc.eventhub.subscription.model.Arbitraries._
import uk.gov.hmrc.eventhub.subscription.model.TestModels.Events._
import uk.gov.hmrc.eventhub.subscription.model.TestModels.Subscriptions._
import uk.gov.hmrc.eventhub.utils.ISpec
import uk.gov.hmrc.eventhub.utils.Setup.scope

import scala.concurrent.ExecutionContext.Implicits.global

class SubscriberPushSubscriptionsISpec extends AnyFlatSpec with ISpec with ScalaCheckDrivenPropertyChecks {

  behavior of "SubscriberPushSubscriptions"

  ignore should "push an event to a registered subscriber" in scope(channelPreferencesBouncedEmails returning OK) {
    setup =>
      val response = setup
        .postToTopic(BoundedEmailsTopic, event)
        .futureValue

      response.status mustBe CREATED

      val channelPreferencesServer = setup
        .subscriberServer(ChannelPreferencesBounced)
        .value

      oneSecond {
        channelPreferencesServer
          .verify(postRequestedFor(urlEqualTo(ChannelPreferencesBouncedPath)).withEventJson(event))
      }
  }

  ignore should "push an event to registered subscribers" in scope(bouncedEmails returning OK) { setup =>
    val response = setup
      .postToTopic(BoundedEmailsTopic, event)
      .futureValue

    response.status mustBe CREATED

    val channelPreferencesServer = setup
      .subscriberServer(ChannelPreferencesBounced)
      .value

    val anotherPartyServer = setup
      .subscriberServer(AnotherPartyBounced)
      .value

    oneSecond {
      channelPreferencesServer
        .verify(postRequestedFor(urlEqualTo(ChannelPreferencesBouncedPath)).withEventJson(event))

      anotherPartyServer
        .verify(postRequestedFor(urlEqualTo(AnotherPartyBouncedPath)).withEventJson(event))
    }
  }

  ignore should "push events to all registered subscribers" in scope(bouncedEmails returning OK) { setup =>
    forAll { eventList: List[Event] =>
      val sentEvents =
        Source(eventList)
          .mapAsyncUnordered(1)(event => setup.postToTopic(BoundedEmailsTopic, event).map(_ -> event))
          .collect { case (result, event) if result.status == CREATED => event }
          .runWith(Sink.seq)(setup.materializer)
          .futureValue

      setup
        .subscribers
        .foreach { subscriber =>
          val server = setup
            .subscriberServer(subscriber.name)
            .value

          oneMinute {
            sentEvents.foreach { event =>
              server
                .verify(
                  postRequestedFor(urlEqualTo(subscriber.uri.path.toString)).withEventJson(event)
                )
            }
          }
        }
    }
  }

  ignore should "apply retry with exponential back-off" in scope(
    channelPreferencesBouncedEmails returning InternalServerError
  ) { setup =>
    forAll { event: Event =>
      val response = setup
        .postToTopic(BoundedEmailsTopic, event)
        .futureValue

      response.status mustBe CREATED

      setup
        .subscribers
        .foreach { subscriber =>
          val server = setup
            .subscriberServer(subscriber.name)
            .value

          oneSecond {
            server
              .verify(
                subscriber.maxRetries + 1,
                postRequestedFor(urlEqualTo(subscriber.uri.path.toString)).withEventJson(event)
              )
          }
        }
    }
  }
}
