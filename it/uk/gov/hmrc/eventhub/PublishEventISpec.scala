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

package uk.gov.hmrc.eventhub

import play.api.http.{ ContentTypes, HeaderNames }
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.{ await, defaultAwaitTimeout }
import play.api.{ Application, Environment, Mode }
import uk.gov.hmrc.eventhub.repository.EventRepository
import java.io.File

class PublishEventISpec extends ISpec {
  override def externalServices: Seq[String] = Seq.empty[String]
  val eventRepository = app.injector.instanceOf[EventRepository]

  private val topics: Map[String, List[Map[String, Any]]] = List(
    "email" ->
      List(
        Map(
          "name" -> "subscriberName",
          "uri"  -> "uri"
        ))).toMap

  override def fakeApplication(): Application =
    GuiceApplicationBuilder(environment = Environment.simple(mode = applicationMode.getOrElse(Mode.Test)))
      .configure("topics" -> topics)
      .overrides(additionalOverrides: _*)
      .build()

  "A POST request to publish/:topic" must {

    "return 201 if event is successfully processed" in {
      val topic = "email"
      val response = wsClient
        .url(resource(s"/event-hub/publish/$topic"))
        .withHttpHeaders((HeaderNames.CONTENT_TYPE, ContentTypes.JSON))
        .post(new File("./it/resources/valid-event.json"))
        .futureValue
      response.status mustBe 201
    }

    "return 201 with DuplicateEvent message if event is already processed" in {
      val topic = "email"
      val response1 = wsClient
        .url(resource(s"/event-hub/publish/$topic"))
        .withHttpHeaders((HeaderNames.CONTENT_TYPE, ContentTypes.JSON))
        .post(new File("./it/resources/valid-event.json"))
        .futureValue
      response1.status mustBe 201

      val response2 = wsClient
        .url(resource(s"/event-hub/publish/$topic"))
        .withHttpHeaders((HeaderNames.CONTENT_TYPE, ContentTypes.JSON))
        .post(new File("./it/resources/valid-event.json"))
        .futureValue
      response2.status mustBe 201

      val events = eventRepository.find("1ebbc004-d2ce-11eb-b8bc-0242ac130003", mongoSetup.eventRepository)
      await(events).size mustBe 1

    }

    "return 404 if topic is not configured" in {
      val topic = "unknown"
      val response = wsClient
        .url(resource(s"/event-hub/publish/$topic"))
        .withHttpHeaders((HeaderNames.CONTENT_TYPE, ContentTypes.JSON))
        .post(new File("./it/resources/valid-event-min.json"))
        .futureValue
      response.status mustBe 404
    }

    "return 201 if no subscribers configured for topic" in {
      val topic = "notConfigured"
      val response = wsClient
        .url(resource(s"/event-hub/publish/$topic"))
        .withHttpHeaders((HeaderNames.CONTENT_TYPE, ContentTypes.JSON))
        .post(new File("./it/resources/valid-event-min.json"))
        .futureValue
      response.status mustBe 201
    }
  }
}
