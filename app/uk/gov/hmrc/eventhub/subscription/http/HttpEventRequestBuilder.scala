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

package uk.gov.hmrc.eventhub.subscription.http

import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest}
import play.api.libs.json.Json
import uk.gov.hmrc.eventhub.config.Subscriber
import uk.gov.hmrc.eventhub.model.Event

trait HttpEventRequestBuilder {
  def build(subscriber: Subscriber, event: Event): HttpRequest =
    HttpRequest.apply(
      method = subscriber.httpMethod,
      uri = subscriber.uri,
      entity = HttpEntity(
        ContentTypes.`application/json`,
        Json.toJson(event).toString()
      )
    )
}

object HttpEventRequestBuilder extends HttpEventRequestBuilder
