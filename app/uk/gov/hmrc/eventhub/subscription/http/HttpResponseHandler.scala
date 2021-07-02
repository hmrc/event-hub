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

import uk.gov.hmrc.eventhub.model.{Event, Subscriber}
import uk.gov.hmrc.eventhub.subscription.http.HttpResponseHandler.EventSendStatus
import uk.gov.hmrc.eventhub.subscription.stream.SubscriberEventHttpResponse

import scala.concurrent.Future

object HttpResponseHandler {
  val ResponseParallelism = 4

  sealed trait SendStatus

  case object Sent extends SendStatus
  case object Failed extends SendStatus

  case class EventSendStatus(event: Event, subscriber: Subscriber, sendStatus: SendStatus)
}

trait HttpResponseHandler {
  def handleResponse(subscriberEventHttpResponse: SubscriberEventHttpResponse): Future[EventSendStatus]
}
