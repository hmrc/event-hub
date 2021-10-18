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

import akka.actor.{ActorSystem, Scheduler}
import akka.pattern.FutureTimeoutSupport
import com.google.inject.{AbstractModule, Provides}
import play.api.Configuration
import play.api.libs.concurrent.AkkaGuiceSupport
import uk.gov.hmrc.eventhub.config.{PublisherConfig, ServiceInstancesConfig, SubscriberStreamConfig, SubscriptionDefaults, Topic}
import uk.gov.hmrc.eventhub.metric.{BoundedTimers, Clock, MetricsReporter, MetricsReporterImpl, Timers}
import uk.gov.hmrc.eventhub.repository.{EventRepository, SubscriberEventRepositoryFactory, WorkItemSubscriberEventRepositoryFactory}
import uk.gov.hmrc.eventhub.service._
import uk.gov.hmrc.eventhub.subscription.SubscriberPushSubscriptions
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.Audit
import uk.gov.hmrc.play.bootstrap.config.AppName

import javax.inject.Singleton

class EventHubModule extends AbstractModule with AkkaGuiceSupport with FutureTimeoutSupport {
  override def configure(): Unit = {
    bind(classOf[MetricsReporter]).to(classOf[MetricsReporterImpl]).asEagerSingleton()

    bind(classOf[SubscriberEventRepositoryFactory])
      .to(classOf[WorkItemSubscriberEventRepositoryFactory])
      .asEagerSingleton()

    bind(classOf[SubscriberPushSubscriptions]).asEagerSingleton()
    bind(classOf[MongoCollections]).to(classOf[MongoSetup]).asEagerSingleton()

    bind(classOf[SubscriptionMatcher]).to(classOf[SubscriptionMatcherImpl]).asEagerSingleton()
    bind(classOf[TransactionHandler]).to(classOf[TransactionHandlerImpl]).asEagerSingleton()
    bind(classOf[EventPublisher]).to(classOf[EventPublisherImpl]).asEagerSingleton()
    bind(classOf[PublishEventAuditor]).to(classOf[PublishEventAuditorImpl]).asEagerSingleton()
    bind(classOf[EventPublisherService]).to(classOf[EventPublisherServiceImpl]).asEagerSingleton()

    super.configure()
  }

  @Provides
  @Singleton
  def eventRepository(mongoSetup: MongoSetup): EventRepository =
    new EventRepository(mongoSetup.eventRepository)

  @Provides
  @Singleton
  def audit(configuration: Configuration, auditConnector: AuditConnector): Audit = {
    val appName = AppName.fromConfiguration(configuration)
    Audit(appName, auditConnector)
  }

  @Provides
  @Singleton
  def timers(configuration: Configuration, actorSystem: ActorSystem): Timers = {
    val maxTimers = configuration.get[Int]("metrics.max-timers")
    val clock = new Clock
    new BoundedTimers(clock, maxTimers)(actorSystem)
  }

  @Provides
  @Singleton
  def scheduler(system: ActorSystem): Scheduler =
    system.scheduler

  @Provides
  @Singleton
  def publisherConfig(configuration: Configuration): PublisherConfig =
    configuration.get[PublisherConfig](path = "publisher-config")

  @Provides
  @Singleton
  def serviceInstancesConfig(configuration: Configuration): ServiceInstancesConfig =
    configuration.get[ServiceInstancesConfig](path = "service-instances-config")

  @Provides
  @Singleton
  def subscriberStreamConfig(configuration: Configuration): SubscriberStreamConfig =
    configuration.get[SubscriberStreamConfig](path = "subscriber-stream-config")

  @Provides
  @Singleton
  def subscriptionDefaults(configuration: Configuration): SubscriptionDefaults =
    configuration.get[SubscriptionDefaults](path = "subscription-defaults")

  @Provides
  @Singleton
  def configTopics(configuration: Configuration, subscriptionDefaults: SubscriptionDefaults): Set[Topic] =
    configuration.get[Set[Topic]](path = "topics")(Topic.configLoader(subscriptionDefaults))
}
