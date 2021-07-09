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

package uk.gov.hmrc.eventhub.subscription.http

import akka.http.scaladsl.model.{ ContentTypes, HttpEntity, HttpMethods, HttpRequest }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.eventhub.model.TestModels

class HttpEventRequestBuilderSpec extends AnyFlatSpec with Matchers {

  behavior of "HttpEventRequestBuilder.build"

  it should "return a http request with a POST method, `application/json` header and an event json body" in new Scope {
    HttpEventRequestBuilder.build(subscriber, event) mustBe HttpRequest(
      method = HttpMethods.POST,
      uri = subscriber.uri,
      entity = HttpEntity(
        ContentTypes.`application/json`,
        Json.toJson(event).toString()
      )
    )
  }

  it should "return a http request with a PUT method, `application/json` header and an event json body" in new Scope {
    HttpEventRequestBuilder.build(idempotentSubscriber, event) mustBe HttpRequest(
      method = HttpMethods.PUT,
      uri = idempotentSubscriber.uri,
      entity = HttpEntity(
        ContentTypes.`application/json`,
        Json.toJson(event).toString()
      )
    )
  }

  trait Scope extends TestModels
}
