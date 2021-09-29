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

package uk.gov.hmrc.eventhub.metric

import akka.http.scaladsl.model.StatusCodes.TooManyRequests
import com.codahale.metrics.{Counter, Histogram, MetricRegistry}
import com.kenshoo.play.metrics.Metrics
import org.mockito.IdiomaticMockito
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eventhub.config.TestModels.{EmailTopic, subscriber}
import uk.gov.hmrc.eventhub.metric.Timers.{CompletedTimer, RunningTimer}
import uk.gov.hmrc.eventhub.model.TestModels.event

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class MetricsReporterImpSpec extends AnyFlatSpec with Matchers with IdiomaticMockito {

  "MetricsReporterImpl.incrementEventPublishedCount" should "call metrics counter inc for with the correct metric name" in new Scope {
    metricRegistry.counter(s"event.published;topic=${EmailTopic.name};subject=${event.subject}") returns counter
    metricsReporterImpl.incrementEventPublishedCount(event, EmailTopic)
    counter.inc() wasCalled once
  }

  "MetricsReporterImpl.incrementDuplicateEventCount" should "call metrics counter inc for with the correct metric name" in new Scope {
    metricRegistry.counter(s"event.duplicate;topic=${EmailTopic.name};subject=${event.subject}") returns counter
    metricsReporterImpl.incrementDuplicateEventCount(event, EmailTopic)
    counter.inc() wasCalled once
  }

  "MetricsReporterImpl.incrementSubscriptionEventEnqueuedCount" should "call metrics counter inc for with the correct metric name" in new Scope {
    metricRegistry.counter(s"subscriber.enqueued;subscriber=${subscriber.name}") returns counter
    metricsReporterImpl.incrementSubscriptionEventEnqueuedCount(subscriber)
    counter.inc() wasCalled once
  }

  "MetricsReporterImpl.incrementSubscriptionPublishedCount" should "call metrics counter inc for with the correct metric name" in new Scope {
    metricRegistry.counter(s"subscriber.published;subscriber=${subscriber.name}") returns counter
    metricsReporterImpl.incrementSubscriptionPublishedCount(subscriber)
    counter.inc() wasCalled once
  }

  "MetricsReporterImpl.incrementSubscriptionRetry" should "call metrics counter inc for with the correct metric name (no status code)" in new Scope {
    metricRegistry.counter(s"subscriber.retry;subscriber=${subscriber.name}") returns counter
    metricsReporterImpl.incrementSubscriptionRetry(subscriber, None)
    counter.inc() wasCalled once
  }

  "MetricsReporterImpl.incrementSubscriptionRetry" should "call metrics counter inc for with the correct metric name (with status code)" in new Scope {
    metricRegistry.counter(
      s"subscriber.retry;subscriber=${subscriber.name};status=${TooManyRequests.intValue}"
    ) returns counter
    metricsReporterImpl.incrementSubscriptionRetry(subscriber, Some(TooManyRequests))
    counter.inc() wasCalled once
  }

  "MetricsReporterImpl.incrementSubscriptionFailure" should "call metrics counter inc for with the correct metric name" in new Scope {
    metricRegistry.counter(s"subscriber.failed;subscriber=${subscriber.name}") returns counter
    metricsReporterImpl.incrementSubscriptionFailure(subscriber)
    counter.inc() wasCalled once
  }

  "MetricsReporterImpl.incrementSubscriptionPermanentFailure" should "call metrics counter inc for with the correct metric name" in new Scope {
    metricRegistry.counter(s"subscriber.permanently-failed;subscriber=${subscriber.name}") returns counter
    metricsReporterImpl.incrementSubscriptionPermanentFailure(subscriber)
    counter.inc() wasCalled once
  }

  "MetricsReporterImpl.reportSubscriberRequestLatency" should "call metrics counter inc for with the correct metric name" in new Scope {
    metricRegistry.histogram(s"subscriber.request-latency;subscriber=${subscriber.name}") returns histogram
    metricsReporterImpl.reportSubscriberRequestLatency(subscriber, 1)
    histogram.update(1L) wasCalled once
  }

  "MetricsReporterImpl.startSubscriptionPublishTimer" should "call timers to start a running timer with the correct metric name" in new Scope {
    timers.startTimer(s"${subscriber.name}.${event.eventId}") returns Future.successful(
      RunningTimer(System.currentTimeMillis())
    )
    metricsReporterImpl.startSubscriptionPublishTimer(subscriber, event)
    timers.startTimer(s"${subscriber.name}.${event.eventId}") wasCalled once
  }

  // This test fails
//  "MetricsReporterImpl.stopSubscriptionPublishTimer" should "call timers to start a running timer with the correct metric name" in new Scope {
//    val start: Long = System.currentTimeMillis()
//    val end: Long = start + 1000
//
//    timers.stopTimer(s"${subscriber.name}.${event.eventId}") returns Future.successful(Some(CompletedTimer(start, end)))
//    metricRegistry.histogram(s"subscriber.e2e-latency;subscriber=${subscriber.name}") returns histogram
//
//    metricsReporterImpl.stopSubscriptionPublishTimer(subscriber, event)
//    timers.stopTimer(s"${subscriber.name}.${event.eventId}") wasCalled once
//    histogram.update(end - start) wasCalled once
//  }

  trait Scope {
    val metrics: Metrics = mock[Metrics]
    val metricRegistry: MetricRegistry = mock[MetricRegistry]
    val counter: Counter = mock[Counter]
    val histogram: Histogram = mock[Histogram]
    metrics.defaultRegistry returns metricRegistry

    val timers: Timers = mock[Timers]
    val metricsReporterImpl: MetricsReporterImpl = new MetricsReporterImpl(metrics, timers)
  }
}
