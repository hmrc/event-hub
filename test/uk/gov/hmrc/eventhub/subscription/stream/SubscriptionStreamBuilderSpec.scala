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

import org.apache.pekko.NotUsed
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import org.mockito.Mockito.{atLeastOnce, verify, when}
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eventhub.cluster.ServiceInstances
import uk.gov.hmrc.eventhub.config.TestModels.*
import uk.gov.hmrc.eventhub.config.{SubscriberStreamBackoffConfig, SubscriberStreamConfig, TopicName}
import uk.gov.hmrc.eventhub.metric.{MetricsReporterImpl, Timers}
import uk.gov.hmrc.eventhub.repository.{SubscriberEventRepository, SubscriberEventRepositoryFactory}
import uk.gov.hmrc.eventhub.subscription.http.HttpResponseHandler.EventSendStatus
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*

class SubscriptionStreamBuilderSpec extends AnyFlatSpec with Matchers with MockitoSugar with ScalaFutures {

  behavior of "SubscriptionStreamBuilder.build"

  it should "build a subscription stream for a given topic and subscriber" in new Scope {
    when(subscriberEventRepositoryFactory.apply(subscriber, TopicName("email")))
      .thenReturn(mock[SubscriberEventRepository])

    subscriptionStreamBuilder
      .build(subscriber, TopicName("email"))
      .isInstanceOf[Source[EventSendStatus, NotUsed]] shouldBe true

    verify(subscriberEventRepositoryFactory, atLeastOnce).apply(subscriber, TopicName("email"))
  }

  trait Scope {
    private implicit val system: ActorSystem = ActorSystem("SubscriptionStreamBuilderSpec")
    implicit val materializer: Materializer = Materializer(system)
    val metrics: Metrics = mock[Metrics]
    val timers: Timers = mock[Timers]
    val metricsReporter = new MetricsReporterImpl(metrics, timers)
    val subscriberEventRepositoryFactory: SubscriberEventRepositoryFactory = mock[SubscriberEventRepositoryFactory]
    val subscriberStreamConfig: SubscriberStreamConfig = SubscriberStreamConfig(
      300.millis,
      SubscriberStreamBackoffConfig(100.millis, 10.minutes)
    )
    val serviceInstances: ServiceInstances = mock[ServiceInstances]

    val subscriptionStreamBuilder = new SubscriptionStreamBuilder(
      subscriberEventRepositoryFactory = subscriberEventRepositoryFactory,
      subscriberStreamConfig = subscriberStreamConfig,
      serviceInstances = serviceInstances,
      metricsReporter = metricsReporter
    )
  }
}
