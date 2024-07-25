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

import org.apache.pekko.http.scaladsl.model.StatusCodes.TooManyRequests
import com.codahale.metrics.{Counter, Gauge, Histogram, MetricRegistry}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{times, verify, when}
import org.mockito.ArgumentMatchers.*
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.Waiters.timeout
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eventhub.config.TestModels.{EmailTopic, subscriber}
import uk.gov.hmrc.eventhub.metric.MetricsReporter.{ExceptionalStatus, HttpStatus}
import uk.gov.hmrc.eventhub.metric.Timers.{CompletedTimer, RunningTimer}
import uk.gov.hmrc.eventhub.model.TestModels.event
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.*

class MetricsReporterImplSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  "MetricsReporterImpl.incrementEventPublishedCount" should "call metrics counter inc for with the correct metric name" in new Scope {
    when(metricRegistry.counter(s"event.published.${EmailTopic.name}.${event.subject}")).thenReturn(counter)
    metricsReporterImpl.incrementEventPublishedCount(event, EmailTopic)
    verify(counter, times(1)).inc()
  }

  "MetricsReporterImpl.incrementDuplicateEventCount" should "call metrics counter inc for with the correct metric name" in new Scope {
    when(metricRegistry.counter(s"event.duplicate.${EmailTopic.name}.${event.subject}")).thenReturn(counter)
    metricsReporterImpl.incrementDuplicateEventCount(event, EmailTopic)
    verify(counter, times(1)).inc()
  }

  "MetricsReporterImpl.incrementSubscriptionEventEnqueuedCount" should "call metrics counter inc for with the correct metric name" in new Scope {
    when(metricRegistry.counter(s"subscriber.enqueued.${subscriber.name}")).thenReturn(counter)
    metricsReporterImpl.incrementSubscriptionEventEnqueuedCount(subscriber)
    verify(counter, times(1)).inc()
  }

  "MetricsReporterImpl.incrementSubscriptionPublishedCount" should "call metrics counter inc for with the correct metric name" in new Scope {
    when(metricRegistry.counter(s"subscriber.published.${subscriber.name}")).thenReturn(counter)
    metricsReporterImpl.incrementSubscriptionPublishedCount(subscriber)
    verify(counter, times(1)).inc()
  }

  "MetricsReporterImpl.incrementSubscriptionRetry" should "call metrics counter inc for with the correct metric name (no status code)" in new Scope {
    when(metricRegistry.counter(s"subscriber.retry.${subscriber.name}.exceptional")).thenReturn(counter)
    metricsReporterImpl.incrementSubscriptionRetry(subscriber, ExceptionalStatus)
    verify(counter, times(1)).inc()
  }

  "MetricsReporterImpl.incrementSubscriptionRetry" should "call metrics counter inc for with the correct metric name (with status code)" in new Scope {
    when(
      metricRegistry.counter(
        s"subscriber.retry.${subscriber.name}.${TooManyRequests.intValue}"
      )
    ).thenReturn(counter)
    metricsReporterImpl.incrementSubscriptionRetry(subscriber, HttpStatus(TooManyRequests))
    verify(counter, times(1)).inc()
  }

  "MetricsReporterImpl.incrementSubscriptionFailure" should "call metrics counter inc for with the correct metric name" in new Scope {
    when(metricRegistry.counter(s"subscriber.failed.${subscriber.name}")).thenReturn(counter)
    metricsReporterImpl.incrementSubscriptionFailure(subscriber)
    verify(counter, times(1)).inc()
  }

  "MetricsReporterImpl.incrementSubscriptionPermanentFailure" should "call metrics counter inc for with the correct metric name" in new Scope {
    when(metricRegistry.counter(s"subscriber.permanently-failed.${subscriber.name}")).thenReturn(counter)
    metricsReporterImpl.incrementSubscriptionPermanentFailure(subscriber)
    verify(counter, times(1)).inc()
  }

  "MetricsReporterImpl.reportSubscriberRequestLatency" should "call metrics counter inc for with the correct metric name" in new Scope {
    when(metricRegistry.histogram(s"subscriber.request-latency.${subscriber.name}")).thenReturn(histogram)
    metricsReporterImpl.reportSubscriberRequestLatency(subscriber, 1)
    verify(histogram, times(1)).update(1L)
  }

  "MetricsReporterImpl.startSubscriptionPublishTimer" should "call timers to start a running timer with the correct metric name" in new Scope {
    when(timers.startTimer(s"${subscriber.name}.${event.eventId}")).thenReturn(
      Future.successful(
        RunningTimer(System.currentTimeMillis())
      )
    )
    metricsReporterImpl.startSubscriptionPublishTimer(subscriber, event)
    verify(timers, times(1)).startTimer(s"${subscriber.name}.${event.eventId}")
  }

  "MetricsReporterImpl.stopSubscriptionPublishTimer" should "call timers to stop a running timer with the correct metric name" in new Scope {
    val start: Long = System.currentTimeMillis()
    val end: Long = start + 1000

    when(timers.stopTimer(s"${subscriber.name}.${event.eventId}")).thenReturn(
      Future.successful(
        Some(CompletedTimer(start, end))
      )
    )
    when(metricRegistry.histogram(s"subscriber.e2e-latency.${subscriber.name}")).thenReturn(histogram)

    metricsReporterImpl.stopSubscriptionPublishTimer(subscriber, event)
    verify(timers, times(1)).stopTimer(s"${subscriber.name}.${event.eventId}")

    eventually(timeout(3.seconds)) {
      verify(histogram, times(1)).update(end - start)
    }
  }

  "MetricsReporterImpl.gaugeServiceInstances" should "provide the service instance count to a gauge with the correct name" in new Scope {
    val name = "service-instances"
    metricsReporterImpl.gaugeServiceInstances(() => 2)
    verify(metricRegistry, times(1)).gauge(ArgumentMatchers.eq(name), any[MetricRegistry.MetricSupplier[Gauge[?]]])
  }

  trait Scope {
    val metrics: Metrics = mock[Metrics]
    val metricRegistry: MetricRegistry = mock[MetricRegistry]
    val counter: Counter = mock[Counter]
    val histogram: Histogram = mock[Histogram]
    when(metrics.defaultRegistry).thenReturn(metricRegistry)

    val timers: Timers = mock[Timers]
    val metricsReporterImpl: MetricsReporterImpl = new MetricsReporterImpl(metrics, timers)
  }
}
