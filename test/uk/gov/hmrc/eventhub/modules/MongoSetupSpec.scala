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

package uk.gov.hmrc.eventhub.modules

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration}
import uk.gov.hmrc.eventhub.config.TestModels.emails
import uk.gov.hmrc.eventhub.config.TopicName
import uk.gov.hmrc.eventhub.model.TestModels.{channelPreferences, toConfigMap}
import uk.gov.hmrc.mongo.MongoComponent

import java.time.{Duration, Instant}
import scala.concurrent.ExecutionContext

class MongoSetupSpec extends AnyFlatSpec with Matchers with ScalaFutures with GuiceOneServerPerSuite {

  lazy val ttlInSecondsEvent = 10
  lazy val ttlInSecondsSubscribers = 12
  val inProgressRetryAfterInHours = 24

  protected def serviceMongoUri =
    s"mongodb://localhost:27017/${getClass.getSimpleName}"

  private lazy val mongoConfig =
    Map(s"mongodb.uri" -> serviceMongoUri)

  def additionalConfig: Map[String, ? <: Any] =
    Map(
      "metrics.enabled"                           -> false,
      "auditing.enabled"                          -> false,
      "publish.workItem.retryAfterHours"          -> inProgressRetryAfterInHours,
      "event-repo.expire-after-seconds-ttl"       -> ttlInSecondsEvent,
      "subscriber-repos.expire-after-seconds-ttl" -> ttlInSecondsSubscribers,
      "topics"                                    -> toConfigMap(channelPreferences, TopicName("email"))
    )

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder().configure(mongoConfig ++ additionalConfig).build()

  behavior of "MongoSetup.collectionName"

  it should "return the composed collection name based on topic and subscription name" in new Scope {
    val sutCollectionName = mongoSetup.collectionName(TopicName("email"), "channel-preferences-bounced")
    sutCollectionName should be("email_channel-preferences-bounced_queue")
  }

  behavior of "MongoSetup.subscriberRepositories"

  it should "return a set of subscriberRepositories based on it's configuration" in new Scope {
    mongoSetup.subscriberRepositories.size should be(2)
  }

  behavior of "MongoSetup.subscriberRepositories(x).now"

  it should "return a set of subscriberRepositories based on it's configuration" in new Scope {
    val oldNow: Long = Instant.now().toEpochMilli
    val sutNow: Long = mongoSetup.subscriberRepositories.head.workItemRepository.now().toEpochMilli
    val newNow: Long = Instant.now().toEpochMilli
    (oldNow <= sutNow && sutNow >= newNow) should be(true)
  }

  behavior of "MongoSetup.subscriberRepositories(x).inProgressRetryAfter"

  it should "return a set of subscriberRepositories based on it's configuration" in new Scope {
    mongoSetup.subscriberRepositories.head.workItemRepository.inProgressRetryAfter should
      be(Duration.ofSeconds(inProgressRetryAfterInHours))
  }

  trait Scope {
    implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
    val mongoComponent: MongoComponent = app.injector.instanceOf[MongoComponent]
    val configuration: Configuration = app.injector.instanceOf[Configuration]
    val mongoSetup = new MongoSetup(mongoComponent, configuration, Set(emails))(ec)
  }
}
