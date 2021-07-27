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

import org.mongodb.scala.model.{ IndexModel, IndexOptions }
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.{ Document, MongoClient, MongoCollection, MongoDatabase, model }
import play.api.Configuration
import play.api.i18n.Lang.logger
import uk.gov.hmrc.eventhub.models.{ Event, Subscriber }
import uk.gov.hmrc.eventhub.repository.EventRepository
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.workitem.{ WorkItemFields, WorkItemRepository }
import java.time.{ Duration, Instant }
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.collection.immutable
import scala.concurrent.{ ExecutionContext, Future }

trait MongoCollections

class MongoSetup @Inject()(mongo: MongoComponent, configuration: Configuration)(implicit ec: ExecutionContext)
    extends MongoCollections {

  val topics: Map[String, List[Subscriber]] = configuration.get[Map[String, List[Subscriber]]]("topics")

  private def collectionName(topic: String, subscriptionName: String) = s"${topic}_${subscriptionName}_queue"
  def subscriberRepositories: Seq[(String, WorkItemRepository[Event])] =
    for {
      topic      <- topics.toList
      subscriber <- topic._2
      name = collectionName(topic._1, subscriber.name)
    } yield {
      (
        topic._1,
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
        })
    }

  def eventRepository: PlayMongoRepository[Event] = {
    val repository = new PlayMongoRepository[Event](
      mongoComponent = mongo,
      "event",
      Event.eventFormat,
      indexes = Seq(IndexModel(ascending("eventId"), IndexOptions().unique(true))),
    )
    repository
  }
}
