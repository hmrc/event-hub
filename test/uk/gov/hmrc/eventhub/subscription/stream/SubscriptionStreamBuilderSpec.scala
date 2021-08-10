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

package uk.gov.hmrc.eventhub.subscription.stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import org.mockito.IdiomaticMockito
import org.mockito.MockitoSugar.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eventhub.cluster.ServiceInstances
import uk.gov.hmrc.eventhub.config.SubscriberStreamConfig
import uk.gov.hmrc.eventhub.repository.{SubscriberEventRepository, SubscriberEventRepositoryFactory}
import uk.gov.hmrc.eventhub.subscription.http.{HttpClient, HttpRetryHandlerImpl}
import uk.gov.hmrc.eventhub.config.TestModels._
import uk.gov.hmrc.eventhub.subscription.http.HttpResponseHandler.EventSendStatus

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class SubscriptionStreamBuilderSpec extends AnyFlatSpec with Matchers with IdiomaticMockito with ScalaFutures {

  behavior of "SubscriptionStreamBuilder.build"

  it should "build a subscription stream for a given topic and subscriber" in new Scope {
    when(subscriberEventRepositoryFactory.apply(subscriber, topic = "email"))
      .thenReturn(mock[SubscriberEventRepository])

    subscriptionStreamBuilder
      .build(subscriber, "email")
      .isInstanceOf[Source[EventSendStatus, NotUsed]] shouldBe true

    subscriberEventRepositoryFactory.apply(subscriber, topic = "email") wasCalled once
  }

  trait Scope {
    private implicit val system: ActorSystem = ActorSystem("SubscriptionStreamBuilderSpec")
    implicit val materializer: Materializer = Materializer(system)
    val httpRetryHandler = new HttpRetryHandlerImpl()
    val subscriberEventRepositoryFactory: SubscriberEventRepositoryFactory = mock[SubscriberEventRepositoryFactory]
    val subscriberStreamConfig: SubscriberStreamConfig = SubscriberStreamConfig(300.millis)
    val serviceInstances: ServiceInstances = mock[ServiceInstances]
    val httpClient: HttpClient = mock[HttpClient]

    val subscriptionStreamBuilder = new SubscriptionStreamBuilder(
      subscriberEventRepositoryFactory = subscriberEventRepositoryFactory,
      subscriberStreamConfig = subscriberStreamConfig,
      serviceInstances = serviceInstances,
      httpClient = httpClient,
      httpRetryHandler = httpRetryHandler
    )
  }
}
