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
import uk.gov.hmrc.eventhub.config.{Subscriber, TopicName}
import uk.gov.hmrc.eventhub.model.Event

object DisabledMetricsReporter extends MetricsReporter {
  override def incrementEventPublishedCount(event: Event, topicName: TopicName): Unit = {}

  override def incrementDuplicateEventCount(event: Event, topicName: TopicName): Unit = {}

  override def incrementSubscriptionEventEnqueuedCount(subscriber: Subscriber): Unit = {}

  override def incrementSubscriptionPublishedCount(subscriber: Subscriber): Unit = {}

  override def incrementSubscriptionRetry(subscriber: Subscriber, failedStatus: MetricsReporter.FailedStatus): Unit = {}

  override def incrementSubscriptionFailure(subscriber: Subscriber): Unit = {}

  override def incrementSubscriptionPermanentFailure(subscriber: Subscriber): Unit = {}

  override def reportSubscriberRequestLatency(subscriber: Subscriber, millis: Long): Unit = {}

  override def startSubscriptionPublishTimer(subscriber: Subscriber, event: Event): Unit = {}

  override def stopSubscriptionPublishTimer(subscriber: Subscriber, event: Event): Unit = {}

  override def gaugeServiceInstances(instanceCount: () => Int): Unit = {}
}
