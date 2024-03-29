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

import org.apache.pekko.http.scaladsl.HttpExt
import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse}
import uk.gov.hmrc.eventhub.config.Subscriber
import uk.gov.hmrc.eventhub.metric.MetricsReporter

import scala.concurrent.{ExecutionContext, Future}

trait HttpClient {
  def singleRequest(httpRequest: HttpRequest): Future[HttpResponse]
}

class PekkoHttpClient(
  httpExt: HttpExt,
  subscriber: Subscriber,
  metricsReporter: MetricsReporter
)(implicit executionContext: ExecutionContext)
    extends HttpClient {
  override def singleRequest(httpRequest: HttpRequest): Future[HttpResponse] = {
    val start = System.currentTimeMillis()
    httpExt
      .singleRequest(httpRequest)
      .map { result =>
        metricsReporter.reportSubscriberRequestLatency(subscriber, System.currentTimeMillis() - start)
        result
      }
  }
}
