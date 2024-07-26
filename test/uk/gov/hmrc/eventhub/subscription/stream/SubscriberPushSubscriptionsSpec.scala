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
import org.apache.pekko.stream.scaladsl.Source
import org.mockito.Mockito.{atLeastOnce, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.eventhub.config.{Subscriber, Topic, TopicName}
import org.mockito.ArgumentMatchers.*
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eventhub.config.TestModels.*

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.eventhub.subscription.SubscriberPushSubscriptions

class SubscriberPushSubscriptionsSpec extends AnyFlatSpec with Matchers with MockitoSugar with ScalaFutures {

  behavior of "SubscriberPushSubscriptions"

  it should "create and run a subscriber stream for single topic with a single subscriber" in new Scope {
    when(subscriptionStreamBuilder.build(any, any[TopicName])).thenReturn(Source.empty)
    subscriberPushSubscriptions(Set(Topic(TopicName("test"), List(subscriber))))

    verify(subscriptionStreamBuilder, atLeastOnce).build(any, any[TopicName])
  }

  it should "create and run subscriber streams for a complex set of topic configurations" in new Scope {
    val subscriberTwo: Subscriber = subscriber.copy(name = "foo-two")
    val subscriberThree: Subscriber = subscriber.copy(name = "foo-three")
    val subscriberFour: Subscriber = subscriber.copy(name = "foo-four")

    val complexTopics: Set[Topic] =
      Set(
        Topic(TopicName("email"), List(subscriber, subscriberTwo)),
        Topic(TopicName("letter"), List(subscriberThree)),
        Topic(TopicName("telephone"), List(subscriberFour)),
        Topic.empty(TopicName("empty"))
      )

    when(subscriptionStreamBuilder.build(any, any[TopicName]))
      .thenReturn(Source.empty, Source.empty, Source.empty, Source.empty)

    subscriberPushSubscriptions(complexTopics)

    verify(subscriptionStreamBuilder, times(4)).build(any, any[TopicName])
  }

  trait Scope {
    val subscriptionStreamBuilder: SubscriptionStreamBuilder = mock[SubscriptionStreamBuilder]
    val lifecycle: ApplicationLifecycle = mock[ApplicationLifecycle]
    private val system = ActorSystem("SubscriberPushSubscriptions")
    implicit val materializer: Materializer = Materializer(system)

    def subscriberPushSubscriptions(topics: Set[Topic]): SubscriberPushSubscriptions = new SubscriberPushSubscriptions(
      topics,
      subscriptionStreamBuilder,
      lifecycle
    )
  }
}
