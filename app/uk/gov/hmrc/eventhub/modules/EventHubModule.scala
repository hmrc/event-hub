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

package uk.gov.hmrc.eventhub.modules

import akka.actor.ActorSystem
import akka.http.scaladsl.{ Http, HttpExt }
import akka.pattern.FutureTimeoutSupport
import com.google.inject.{ AbstractModule, Provides }
import play.api.Configuration
import play.api.libs.concurrent.AkkaGuiceSupport
import uk.gov.hmrc.eventhub.config.SubscriberStreamConfig
import uk.gov.hmrc.eventhub.model.{ Subscriber, Topic }
import uk.gov.hmrc.eventhub.repository.{ SubscriberEventRepositoryFactory, WorkItemSubscriberEventRepositoryFactory }
import uk.gov.hmrc.eventhub.subscription.SubscriberPushSubscriptions

import javax.inject.Singleton

class EventHubModule extends AbstractModule with AkkaGuiceSupport with FutureTimeoutSupport {
  override def configure(): Unit = {
    bind(classOf[SubscriberEventRepositoryFactory])
      .to(classOf[WorkItemSubscriberEventRepositoryFactory])

    bind(classOf[SubscriberPushSubscriptions]).asEagerSingleton()

    bind(classOf[MongoCollections]).to(classOf[MongoSetup])

    super.configure()
  }

  @Provides
  @Singleton
  def createHttpExt(system: ActorSystem): HttpExt =
    Http()(system)

  @Provides
  @Singleton
  def configTopics(configuration: Configuration): Set[Topic] =
    configuration
      .get[Map[String, List[Subscriber]]](path = "topics")
      .map { case (k, v) => Topic(k, v) }
      .toSet

  @Provides
  @Singleton
  def subscriberStreamConfig(configuration: Configuration): SubscriberStreamConfig =
    configuration
      .get[SubscriberStreamConfig](path = "subscriber-stream-config")
}
