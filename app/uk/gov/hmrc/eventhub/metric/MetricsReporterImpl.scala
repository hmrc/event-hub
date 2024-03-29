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

import com.codahale.metrics.Gauge
import uk.gov.hmrc.eventhub.config.{Subscriber, TopicName}
import uk.gov.hmrc.eventhub.metric.MetricsReporter.{EventMetricsOps, FailedStatus, SubscriberMetricsOps}
import uk.gov.hmrc.eventhub.model.Event
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class MetricsReporterImpl @Inject() (metrics: Metrics, timers: Timers)(implicit executionContext: ExecutionContext)
    extends MetricsReporter {

  override def incrementEventPublishedCount(event: Event, topicName: TopicName): Unit =
    incrementCounter(event.metricFor(topicName, "published"))

  override def incrementDuplicateEventCount(event: Event, topicName: TopicName): Unit =
    incrementCounter(event.metricFor(topicName, "duplicate"))

  override def incrementSubscriptionEventEnqueuedCount(subscriber: Subscriber): Unit =
    incrementCounter(subscriber.metricFor("enqueued"))

  override def incrementSubscriptionPublishedCount(subscriber: Subscriber): Unit =
    incrementCounter(subscriber.metricFor("published"))

  override def incrementSubscriptionRetry(subscriber: Subscriber, failedStatus: FailedStatus): Unit =
    incrementCounter(subscriber.metricFor("retry", failedStatus))

  override def incrementSubscriptionFailure(subscriber: Subscriber): Unit =
    incrementCounter(subscriber.metricFor("failed"))

  override def incrementSubscriptionPermanentFailure(subscriber: Subscriber): Unit =
    incrementCounter(subscriber.metricFor("permanently-failed"))

  private def incrementCounter(metricName: String): Unit =
    metrics
      .defaultRegistry
      .counter(metricName)
      .inc()

  override def reportSubscriberRequestLatency(subscriber: Subscriber, millis: Long): Unit =
    metrics
      .defaultRegistry
      .histogram(subscriber.metricFor("request-latency"))
      .update(millis)

  override def startSubscriptionPublishTimer(subscriber: Subscriber, event: Event): Unit =
    timers.startTimer(subscriberEventTimerName(subscriber, event))

  override def stopSubscriptionPublishTimer(subscriber: Subscriber, event: Event): Unit =
    timers
      .stopTimer(subscriberEventTimerName(subscriber, event))
      .map(
        _.map(completedTimer =>
          metrics
            .defaultRegistry
            .histogram(subscriber.metricFor("e2e-latency"))
            .update(completedTimer.time)
        )
      )

  private def subscriberEventTimerName(subscriber: Subscriber, event: Event): String =
    s"${subscriber.name}.${event.eventId}"

  override def gaugeServiceInstances(instanceCount: () => Int): Unit =
    metrics
      .defaultRegistry
      .gauge(
        "service-instances",
        () =>
          new Gauge[Int] {
            override def getValue: Int = instanceCount()
          }
      )
}
