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

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import org.mockito.IdiomaticMockito
import org.mockito.MockitoSugar.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.eventhub.config.{Subscriber, Topic, TopicName}
import org.mockito.ArgumentMatchersSugar.{*, any}
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eventhub.config.TestModels._

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.eventhub.subscription.SubscriberPushSubscriptions

class SubscriberPushSubscriptionsSpec extends AnyFlatSpec with Matchers with IdiomaticMockito with ScalaFutures {

  behavior of "SubscriberPushSubscriptions"

  it should "create and run a subscriber stream for single topic with a single subscriber" in new Scope {
    when(subscriptionStreamBuilder.build(*, any[TopicName])).thenReturn(Source.empty)
    subscriberPushSubscriptions(Set(Topic(TopicName("test"), List(subscriber))))

    subscriptionStreamBuilder.build(*, any[TopicName]) wasCalled once
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

    when(subscriptionStreamBuilder.build(*, any[TopicName]))
      .thenReturn(Source.empty, Source.empty, Source.empty, Source.empty)

    subscriberPushSubscriptions(complexTopics)

    subscriptionStreamBuilder.build(*, any[TopicName]) wasCalled fourTimes
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
