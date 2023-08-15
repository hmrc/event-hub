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

package uk.gov.hmrc.eventhub.repository

import org.mockito.IdiomaticMockito
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.{Application, Configuration}
import uk.gov.hmrc.eventhub.config.TopicName
import uk.gov.hmrc.eventhub.model.TestModels.channelPreferences
import uk.gov.hmrc.eventhub.modules.MongoSetup
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.ExecutionContext

class WorkItemSubscriberEventRepositoryFactorySpec
    extends AnyFlatSpec with Matchers with IdiomaticMockito with ScalaFutures with GuiceOneServerPerSuite {

  protected def serviceMongoUri =
    s"mongodb://localhost:27017/${getClass.getSimpleName}"

  private lazy val mongoConfig =
    Map(s"mongodb.uri" -> serviceMongoUri)

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(mongoConfig).build()

  behavior of "WorkItemSubscriberEventRepositoryFactory.apply"

  it should "instantiate a new WorkItemSubscriberEventRepository from a SubscriberQueueRepository" in new Scope {
    val factory = new WorkItemSubscriberEventRepositoryFactory(configuration, mongoComponent)(ec)
    val sut = factory.apply(channelPreferences, topicName)
    sut.isInstanceOf[SubscriberEventRepository] should be(true)
    sut.isInstanceOf[WorkItemSubscriberEventRepository] should be(true)
  }

  trait Scope {
    implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
    val wsClient: WSClient = app.injector.instanceOf[WSClient]
    val mongoSetup: MongoSetup = app.injector.instanceOf[MongoSetup]
    val mongoComponent: MongoComponent = app.injector.instanceOf[MongoComponent]
    val configuration: Configuration = app.injector.instanceOf[Configuration]
    val topicName: TopicName = TopicName(name = "topicName")

    val subscriberQueueRepository: SubscriberQueueRepository =
      new SubscriberQueueRepository(topicName, channelPreferences, configuration, mongoComponent)
  }
}
