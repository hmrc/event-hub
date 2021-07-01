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

package uk.gov.hmrc.eventhub.subscriptions.stream

import akka.NotUsed
import akka.stream.scaladsl.Source
import play.api.Logging
import uk.gov.hmrc.eventhub.subscriptions.http.HttpResponseHandler
import uk.gov.hmrc.eventhub.subscriptions.http.HttpResponseHandler.{EventSendStatus, ResponseParallelism}

object PushSubscription extends Logging {
  def subscriptionStream(
    subscriberEventSource: SubscriberEventSource,
    subscriberEventHttpFlow: SubscriberEventHttpFlow,
    httpResponseHandler: HttpResponseHandler
  ): Source[EventSendStatus, NotUsed] =
    subscriberEventSource
      .source
      .via(subscriberEventHttpFlow.flow)
      .mapAsync(parallelism = ResponseParallelism)(httpResponseHandler.handleResponse)
}
