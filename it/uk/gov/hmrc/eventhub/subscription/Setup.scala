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

import akka.http.scaladsl.model.HttpMethods
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{ aResponse, post, put, urlEqualTo }
import com.github.tomakehurst.wiremock.common.Slf4jNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.github.tomakehurst.wiremock.matching.UrlPattern
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.{ WSClient, WSResponse }
import play.api.test.{ DefaultTestServerFactory, RunningServer }
import uk.gov.hmrc.eventhub.model.{ Event, Topic }
import uk.gov.hmrc.integration.UrlHelper.-/
import org.scalatest.concurrent.ScalaFutures._
import play.api.libs.json.Json

import scala.concurrent.Future

object Setup {
  def scope(topics: Set[Topic])(test: Setup => Any) {
    val setup = new Setup(topics)
    try {
      test(setup)
    } finally setup.shutdown()
  }
}

class Setup private (topics: Set[Topic]) {
  private val OK = 200

  val subscriberServers: Set[SubscriberServers] = topics.map { topic =>
    val subscriberServers = topic.subscribers.map { subscriber =>
      val server = new WireMockServer(
        wireMockConfig()
          .dynamicHttpsPort()
          .dynamicPort()
          .notifier(new Slf4jNotifier(true))
      )

      server.start()

      val stubMethod = if (subscriber.httpMethod == HttpMethods.POST) post(_: UrlPattern) else put(_: UrlPattern)

      server.addStubMapping(
        stubMethod(urlEqualTo(subscriber.uri.path.toString())).willReturn(aResponse().withStatus(OK)).build()
      )

      server -> subscriber.copy(
        uri = subscriber.uri
          .withPort(if (subscriber.uri.scheme == "https") server.httpsPort() else server.port())
      )
    }
    SubscriberServers(topic.name, subscriberServers)
  }

  private val topicsConfig = subscriberServers.map { topic =>
    topic.topicName -> topic.subscriberServers.map {
      case (_, subscriber) =>
        Map(
          "name"         -> subscriber.name,
          "uri"          -> subscriber.uri.toString(),
          "http-method"  -> subscriber.httpMethod.value,
          "elements"     -> subscriber.elements,
          "per"          -> subscriber.per.toString(),
          "min-back-off" -> subscriber.minBackOff.toString(),
          "max-back-off" -> subscriber.maxBackOff.toString(),
          "max-retries"  -> subscriber.maxRetries
        )
    }
  }.toMap

  private val application: Application = new GuiceApplicationBuilder()
    .configure("metrics.enabled" -> false)
    .configure("auditing.enabled" -> false)
    .configure("topics" -> topicsConfig)
    .build()

  private val runningServer: RunningServer = DefaultTestServerFactory.start(application)

  private val port: Int = runningServer.endpoints.httpEndpoint
    .fold(throw new IllegalStateException("No HTTP port available for test server"))(_.port)

  val client: WSClient = application.injector.instanceOf[WSClient]

  def resource(path: String): String = s"http://localhost:$port/${-/(path)}"

  def postToTopic(topicName: String, event: Event): Future[WSResponse] =
    client
      .url(resource(s"/event-hub/publish/$topicName"))
      .withHttpHeaders("Content-Type" -> "application/json")
      .post(Json.toJson(event))

  def subscriberServer(subscriptionName: String): Option[WireMockServer] =
    subscriberServers
      .flatMap(_.subscriberServers.find(_._2.name == subscriptionName).map(_._1))
      .headOption

  def shutdown(): Unit = {
    subscriberServers.foreach(_.subscriberServers.foreach(_._1.stop()))
    runningServer.stopServer.close()
    application.stop().futureValue
  }
}
