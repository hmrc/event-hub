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

package uk.gov.hmrc.eventhub.metric

import akka.http.scaladsl.model.StatusCode
import uk.gov.hmrc.eventhub.config.{Subscriber, TopicName}
import uk.gov.hmrc.eventhub.metric.MetricsReporter.FailedStatus
import uk.gov.hmrc.eventhub.model.Event

trait MetricsReporter {
  def incrementEventPublishedCount(event: Event, topicName: TopicName): Unit
  def incrementDuplicateEventCount(event: Event, topicName: TopicName): Unit
  def incrementSubscriptionEventEnqueuedCount(subscriber: Subscriber): Unit
  def incrementSubscriptionPublishedCount(subscriber: Subscriber)
  def incrementSubscriptionRetry(subscriber: Subscriber, failedStatus: FailedStatus): Unit
  def incrementSubscriptionFailure(subscriber: Subscriber): Unit
  def incrementSubscriptionPermanentFailure(subscriber: Subscriber): Unit
  def reportSubscriberRequestLatency(subscriber: Subscriber, millis: Long): Unit
  def startSubscriptionPublishTimer(subscriber: Subscriber, event: Event): Unit
  def stopSubscriptionPublishTimer(subscriber: Subscriber, event: Event): Unit
  def gaugeServiceInstances(instanceCount: () => Int): Unit
}

object MetricsReporter {

  sealed trait FailedStatus {
    val value: String
  }
  case object ExceptionalStatus extends FailedStatus {
    override val value: String = "exceptional"
  }
  case class HttpStatus(statusCode: StatusCode) extends FailedStatus {
    override val value: String = statusCode.intValue().toString
  }

  implicit class SubscriberMetricsOps(val subscriber: Subscriber) extends AnyVal {
    def metricFor(metricName: String): String = s"subscriber.$metricName.${subscriber.name}"
    def metricFor(metricName: String, status: FailedStatus): String =
      s"${metricFor(metricName)}.${status.value}"
  }

  implicit class EventMetricsOps(val event: Event) extends AnyVal {
    def metricFor(topicName: TopicName, metricName: String): String =
      s"event.$metricName.${topicName.name}.${event.subject}"
  }
}
