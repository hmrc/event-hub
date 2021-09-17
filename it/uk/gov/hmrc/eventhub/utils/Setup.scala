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

package uk.gov.hmrc.eventhub.utils

import akka.stream.Materializer
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.Slf4jNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.scalatest.concurrent.ScalaFutures._
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.test.{DefaultTestServerFactory, RunningServer}
import uk.gov.hmrc.eventhub.config.Subscriber
import uk.gov.hmrc.eventhub.model.Event
import uk.gov.hmrc.eventhub.subscription.SubscriberConfigOps
import uk.gov.hmrc.eventhub.subscription.model.{SubscriberServers, SubscriberStub, TestTopic}
import uk.gov.hmrc.integration.UrlHelper.-/
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.Future

object Setup {
  def scope(testTopic: TestTopic)(test: Setup => Any)(implicit testId: TestId): Unit =
    scope(Set(testTopic))(test)

  def scope(testTopics: Set[TestTopic])(test: Setup => Any)(implicit testId: TestId) {
    val setup = new Setup(testTopics, testId)
    try test(setup)
    finally setup.shutdown()
  }
}

class Setup private (testTopics: Set[TestTopic], testId: TestId) {
  private val subscriberServers: Set[SubscriberServers] = testTopics.map { topic =>
    val subscriberServers = topic.subscriberStubs.map { case SubscriberStub(subscriber, stubMapping) =>
      val server = new WireMockServer(
        wireMockConfig().dynamicHttpsPort().dynamicPort().notifier(new Slf4jNotifier(true))
      )

      server.start()
      server.addStubMapping(stubMapping)
      server -> subscriber.copy(
        uri = subscriber.uri.withPort(if (subscriber.uri.scheme == "https") server.httpsPort() else server.port())
      )
    }
    SubscriberServers(topic.name, subscriberServers)
  }

  private val topicsConfig = subscriberServers.flatMap { topic =>
    topic
      .subscriberServers
      .flatMap { case (_, subscriber) => subscriber.asConfigMap(topic.topicName) }
      .toMap
  }.toMap

  private val application: Application = new GuiceApplicationBuilder()
    .configure("mongodb.uri" -> s"mongodb://localhost:27017/${testId.id}")
    .configure("metrics.enabled" -> false)
    .configure("auditing.enabled" -> false)
    .configure("topics" -> topicsConfig)
    .build()

  private val runningServer: RunningServer = DefaultTestServerFactory.start(application)

  private val port: Int = runningServer
    .endpoints
    .httpEndpoint
    .fold(throw new IllegalStateException("No HTTP port available for test server"))(_.port)

  private val client: WSClient = application.injector.instanceOf[WSClient]
  private val mongoComponent: MongoComponent = application.injector.instanceOf[MongoComponent]

  val subscribers: Set[Subscriber] = subscriberServers.flatMap(_.subscriberServers.map(_._2))

  val materializer: Materializer = application.injector.instanceOf[Materializer]

  def postToTopic(topicName: String, event: Event): Future[WSResponse] =
    client
      .url(s"http://localhost:$port/event-hub/publish/${-/(topicName)}")
      .withHttpHeaders("Content-Type" -> "application/json")
      .post(Json.toJson(event))

  def subscriberServer(subscriptionName: String): Option[WireMockServer] =
    subscriberServers.flatMap(_.subscriberServers.find(_._2.name == subscriptionName).map(_._1)).headOption

  private def shutdown(): Unit = {
    subscriberServers.foreach(_.subscriberServers.foreach(_._1.stop()))

    mongoComponent.database.drop().toFuture().futureValue

    runningServer.stopServer.close()

    application.stop().futureValue
  }
}
