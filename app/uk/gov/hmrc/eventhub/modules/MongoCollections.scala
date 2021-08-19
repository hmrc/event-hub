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

import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import play.api.Configuration
import uk.gov.hmrc.eventhub.config.{Subscriber, Topic}
import uk.gov.hmrc.eventhub.model.{Event, SubscriberRepository}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.workitem.{WorkItemFields, WorkItemRepository}

import java.time.{Duration, Instant}
import javax.inject.Inject
import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext

trait MongoCollections

class MongoSetup @Inject() (mongo: MongoComponent, configuration: Configuration, val topics: Set[Topic])(implicit
  ec: ExecutionContext
) extends MongoCollections {

  val duplicateConfigurationKeys = {

    val allLines = scala.io.Source.fromFile("conf/application.conf").getLines().toList
    val getLinesThatStartWithTopic = allLines.filter(line => line.startsWith("topics"))

    val list = ListBuffer.empty[String]
    getLinesThatStartWithTopic.foreach { line =>
      val indexOf = line.indexOf(":")
      if (indexOf != -1) {
        list.append(line.substring(0, indexOf).trim)
      }
    }

    val valid = list.size == list.distinct.size
    if (!valid) throw new DuplicateConfigurationKeyException
  }

  def collectionName(topic: String, subscriptionName: String): String = s"${topic}_${subscriptionName}_queue"

  def subscriberRepositories: Set[SubscriberRepository] =
    for {
      topic      <- topics
      subscriber <- topic.subscribers
      name = collectionName(topic.name, subscriber.name)
    } yield (
      SubscriberRepository(
        topic.name,
        subscriber,
        new WorkItemRepository[Event](
          name,
          mongo,
          Event.eventFormat,
          WorkItemFields.default,
          false
        ) {
          override def inProgressRetryAfter: Duration =
            Duration.ofSeconds(configuration.underlying.getInt("publish.workItem.retryAfterHours"))
          override def now(): Instant = Instant.now()
        }
      )
    )

  def eventRepository: PlayMongoRepository[Event] = {
    val repository = new PlayMongoRepository[Event](
      mongoComponent = mongo,
      "event",
      Event.eventFormat,
      indexes = Seq(IndexModel(ascending("eventId"), IndexOptions().unique(true)))
    )
    repository
  }
}

class DuplicateConfigurationKeyException extends Exception
