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

package uk.gov.hmrc.eventhub

import play.api.http.{ContentTypes, HeaderNames}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.eventhub.config.TopicName
import uk.gov.hmrc.eventhub.repository.EventRepository
import uk.gov.hmrc.eventhub.subscription.SubscriberConfigOps
import uk.gov.hmrc.eventhub.subscription.model.TestModels.Subscriptions.channelPreferences
import play.api.libs.ws.readableAsString

import java.io.File
import java.util.UUID

class PublishEventISpec extends ISpec {
  lazy val eventRepository: EventRepository = app.injector.instanceOf[EventRepository]

  override def additionalConfig: Map[String, ? <: Any] =
    Map(
      "application.router" -> "testOnlyDoNotUseInAppConf.Routes",
      "metrics.enabled"    -> true,
      "auditing.enabled"   -> false,
      "topics"             -> channelPreferences.asConfigMap(TopicName("email"))
    )

  "A POST request to publish/:topic" should {

    "return 201 if event is successfully processed" in {
      val topic = "email"
      val response = wsClient
        .url(resource(s"/event-hub/publish/$topic"))
        .withHttpHeaders((HeaderNames.CONTENT_TYPE, ContentTypes.JSON))
        .post(new File("./test/resources/valid-event.json"))
        .futureValue
      response.status mustBe 201
      response.body mustBe """{"publishedSubscribers":["channel-preferences-bounced"]}"""
    }

    "return 201 with DuplicateEvent message if event is already processed" in {
      val topic = "email"
      val response1 = wsClient
        .url(resource(s"/event-hub/publish/$topic"))
        .withHttpHeaders((HeaderNames.CONTENT_TYPE, ContentTypes.JSON))
        .post(new File("./test/resources/valid-event.json"))
        .futureValue
      response1.status mustBe 201

      val response2 = wsClient
        .url(resource(s"/event-hub/publish/$topic"))
        .withHttpHeaders((HeaderNames.CONTENT_TYPE, ContentTypes.JSON))
        .post(new File("./test/resources/valid-event.json"))
        .futureValue
      response2.status mustBe 201

      val events = eventRepository.find(UUID.fromString("1ebbc004-d2ce-11eb-b8bc-0242ac130003"))
      await(events).size mustBe 1
    }

    "return 404 if topic is not configured" in {
      val topic = "unknown"
      val response = wsClient
        .url(resource(s"/event-hub/publish/$topic"))
        .withHttpHeaders((HeaderNames.CONTENT_TYPE, ContentTypes.JSON))
        .post(new File("./test/resources/valid-event-min.json"))
        .futureValue
      response.status mustBe 404
    }
  }
}
