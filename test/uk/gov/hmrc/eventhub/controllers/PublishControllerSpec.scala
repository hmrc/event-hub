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

package uk.gov.hmrc.eventhub.controllers

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.testkit.NoMaterializer
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.{ContentTypes, Status}
import play.api.libs.json.Json
import play.api.test.Helpers.{CONTENT_TYPE, contentAsString, defaultAwaitTimeout, status}
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import uk.gov.hmrc.eventhub.config.TestModels.channelPreferences
import uk.gov.hmrc.eventhub.config.TopicName
import uk.gov.hmrc.eventhub.model.*
import uk.gov.hmrc.eventhub.service.EventPublisherService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PublishControllerSpec extends AnyWordSpec with Matchers with MockitoSugar {

  "createEvent" must {
    "return Created if payload id valid and publish response is success" in new TestSetup {
      when(publisherServiceMock.publish(any[Event], any[TopicName]))
        .thenReturn(Future.successful(Right(Set(channelPreferences))))

      val controller: PublishController =
        new PublishController(Helpers.stubControllerComponents(), publisherServiceMock)

      private val fakeRequest =
        FakeRequest(
          "POST",
          routes.PublishController.publish("email").url,
          FakeHeaders(Seq(CONTENT_TYPE -> ContentTypes.JSON)),
          Json.parse(validPayload)
        )

      val result = controller.publish("email")(fakeRequest)
      status(result) shouldBe Status.CREATED
      contentAsString(result) shouldBe s"""{"publishedSubscribers":["${channelPreferences.name}"]}"""
    }

    "return Created if publish response is Duplicate" in new TestSetup {
      when(publisherServiceMock.publish(any[Event], any[TopicName]))
        .thenReturn(Future.successful(Left(DuplicateEvent("Event with Id already exists"))))

      val controller: PublishController =
        new PublishController(Helpers.stubControllerComponents(), publisherServiceMock)

      private val fakeRequest =
        FakeRequest(
          "POST",
          routes.PublishController.publish("email").url,
          FakeHeaders(Seq(CONTENT_TYPE -> ContentTypes.JSON)),
          Json.parse(validPayload)
        )

      val result = controller.publish("email")(fakeRequest)
      status(result) shouldBe Status.CREATED
      contentAsString(result) shouldBe "Event with Id already exists"
    }

    "return NotFound if publish response is NoEventTopic" in new TestSetup {
      when(publisherServiceMock.publish(any[Event], any[TopicName]))
        .thenReturn(Future.successful(Left(NoEventTopic("No topic exits"))))

      val controller: PublishController =
        new PublishController(Helpers.stubControllerComponents(), publisherServiceMock)

      private val fakeRequest =
        FakeRequest(
          "POST",
          routes.PublishController.publish("email").url,
          FakeHeaders(Seq(CONTENT_TYPE -> ContentTypes.JSON)),
          Json.parse(validPayload)
        )

      val result = controller.publish("email")(fakeRequest)
      status(result) shouldBe Status.NOT_FOUND
      contentAsString(result) shouldBe "No topic exits"
    }

    "return Created if publish response is NoSubscriberForTopic" in new TestSetup {
      when(publisherServiceMock.publish(any[Event], any[TopicName]))
        .thenReturn(Future.successful(Left(NoSubscribersForTopic("No subscribers for topic"))))

      val controller: PublishController =
        new PublishController(Helpers.stubControllerComponents(), publisherServiceMock)

      private val fakeRequest =
        FakeRequest(
          "POST",
          routes.PublishController.publish("email").url,
          FakeHeaders(Seq(CONTENT_TYPE -> ContentTypes.JSON)),
          Json.parse(validPayload)
        )

      val result = controller.publish("email")(fakeRequest)
      status(result) shouldBe Status.CREATED
      contentAsString(result) shouldBe "No subscribers for topic"
    }

    "return InternalServerError if publish response is Not known" in new TestSetup {
      when(publisherServiceMock.publish(any[Event], any[TopicName]))
        .thenReturn(Future.successful(Left(UnknownError("unknown error"))))

      val controller: PublishController =
        new PublishController(Helpers.stubControllerComponents(), publisherServiceMock)

      private val fakeRequest =
        FakeRequest(
          "POST",
          routes.PublishController.publish("email").url,
          FakeHeaders(Seq(CONTENT_TYPE -> ContentTypes.JSON)),
          Json.parse(validPayload)
        )

      val result = controller.publish("email")(fakeRequest)
      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      contentAsString(result) shouldBe "unknown error"
    }

    "return BadRequest with message if event payload id invalid" in new TestSetup {
      val controller: PublishController =
        new PublishController(Helpers.stubControllerComponents(), publisherServiceMock)
      val fakeRequest =
        FakeRequest(
          "POST",
          routes.PublishController.publish("email").url,
          FakeHeaders(Seq(CONTENT_TYPE -> ContentTypes.JSON)),
          Json.parse(inValidPayload)
        )

      val result = controller.publish("email")(fakeRequest)
      status(result) shouldBe Status.BAD_REQUEST
      contentAsString(result) should include("Invalid Event payload:")
    }

  }

  class TestSetup {
    val publisherServiceMock = mock[EventPublisherService]

    implicit val mat: Materializer = NoMaterializer

    val validPayload = """{
                         |"eventId":"1ebbc004-d2ce-11eb-b8bc-0242ac130003",
                         |"subject":"subject",
                         |"groupId":"",
                         |"timestamp":"2021-02-11T23:00:00.000Z",
                         |"event": {
                         |"status":"Failed",
                         |"emailAddress":"test@test.com",
                         |"detected":"2021-01-11T23:00:00.000Z",
                         |"code":2,
                         |"reason":"Not delivering to previously bounced address",
                         |"enrolment":"HMRC-MTD-VAT~VRN~GB123456789"
                         |}
                         |}""".stripMargin

    val inValidPayload = """{
                           |"subject":"subject",
                           |"groupId":"",
                           |"timestamp":"2021-02-11T23:00:00.000Z",
                           |"event": {
                           |"status":"Failed"
                           |}
                           |}""".stripMargin
  }
}
