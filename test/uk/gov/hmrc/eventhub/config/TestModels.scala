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

package uk.gov.hmrc.eventhub.config

import akka.http.scaladsl.model.{HttpMethods, Uri}
import scala.concurrent.duration._

object TestModels {
  val Elements = 60
  val MaxConnections = 4

  val subscriptionDefaults: SubscriptionDefaults = SubscriptionDefaults(
    elements = Elements,
    per = 1.second,
    maxConnections = MaxConnections,
    minBackOff = 1.second,
    maxBackOff = 2.seconds,
    maxRetries = 2
  )

  val subscriber: Subscriber = Subscriber(
    name = "foo-subscriber",
    uri = Uri("http://localhost:8080/foo"),
    httpMethod = SubscriptionDefaults.DefaultHttpMethod,
    elements = subscriptionDefaults.elements,
    per = subscriptionDefaults.per,
    maxConnections = MaxConnections,
    minBackOff = subscriptionDefaults.minBackOff,
    maxBackOff = subscriptionDefaults.maxBackOff,
    maxRetries = subscriptionDefaults.maxRetries
  )

  val idempotentSubscriber: Subscriber = subscriber.copy(
    httpMethod = HttpMethods.PUT,
    uri = Uri("http://localhost:8081/foo")
  )
}