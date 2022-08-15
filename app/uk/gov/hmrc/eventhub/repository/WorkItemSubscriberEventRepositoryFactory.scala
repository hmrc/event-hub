/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.eventhub.repository

import play.api.Configuration
import uk.gov.hmrc.eventhub.config.{Subscriber, TopicName}
import uk.gov.hmrc.mongo.MongoComponent

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class WorkItemSubscriberEventRepositoryFactory @Inject() (
  configuration: Configuration,
  mongo: MongoComponent
)(implicit executionContext: ExecutionContext)
    extends SubscriberEventRepositoryFactory {
  override def apply(subscriber: Subscriber, topicName: TopicName): SubscriberEventRepository = {
    val queue = new SubscriberQueueRepository(topicName, subscriber, configuration, mongo)
    new WorkItemSubscriberEventRepository(queue)
  }
}
