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

package uk.gov.hmrc.eventhub.modules

import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import play.api.Configuration
import play.api.i18n.Lang.logger
import uk.gov.hmrc.eventhub.config.{Topic, TopicName}
import uk.gov.hmrc.eventhub.model.{Event, PublishedEvent, SubscriberRepository}
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.workitem.{WorkItemFields, WorkItemRepository}
import uk.gov.hmrc.mongo.{MongoComponent, MongoUtils}

import java.time.{Duration, Instant}
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait MongoCollections

class MongoSetup @Inject() (mongo: MongoComponent, configuration: Configuration, val topics: Set[Topic])(implicit
  ec: ExecutionContext
) extends MongoCollections {

  def collectionName(topicName: TopicName, subscriptionName: String): String =
    s"${topicName.name}_${subscriptionName}_queue"

  val subscriberRepositories: Set[SubscriberRepository] =
    for {
      topic      <- topics
      subscriber <- topic.subscribers
      name = collectionName(topic.name, subscriber.name)
    } yield SubscriberRepository(
      topic.name,
      subscriber,
      new WorkItemRepository[Event](
        name,
        mongo,
        Event.eventFormat,
        WorkItemFields.default,
        replaceIndexes = false
      ) {

        lazy val expireAfterSecondsTTL: Int = {
          val DEFAULT_TTL_SECONDS: Int = 86400 // defaulting to a 24h period
          val ttl = configuration
            .getOptional[Int]("subscriber-repos.expire-after-seconds-ttl")
            .getOrElse(DEFAULT_TTL_SECONDS)
          logger.info(s"subscriberRepositories.expireAfterSecondsTTL: $ttl")
          ttl
        }

        lazy val augmentedIndexes = Seq(
          IndexModel(
            ascending("updatedAt"),
            IndexOptions()
              .name("updatedAtTtlIndex")
              .partialFilterExpression(BsonDocument("status" -> "permanently-failed"))
              .expireAfter(expireAfterSecondsTTL, TimeUnit.SECONDS)
          ),
          IndexModel(
            ascending("item.eventId"),
            IndexOptions().background(true)
          )
        ) ++ indexes

        override def ensureIndexes: Future[Seq[String]] =
          MongoUtils.ensureIndexes(collection, augmentedIndexes, true)

        override def inProgressRetryAfter: Duration =
          Duration.ofSeconds(configuration.underlying.getInt("publish.workItem.retryAfterHours"))
        override def now(): Instant = Instant.now()
      }
    )

  def eventRepository: PlayMongoRepository[PublishedEvent] = {

    lazy val expireAfterSecondsTTL: Int = {
      val DEFAULT_TTL_SECONDS: Int = 86400 // defaulting to a 24h period
      val ttl = configuration
        .getOptional[Int]("event-repo.expire-after-seconds-ttl")
        .getOrElse(DEFAULT_TTL_SECONDS)
      logger.info(s"eventRepository.expireAfterSecondsTTL: $ttl")
      ttl
    }

    val repository = new PlayMongoRepository[PublishedEvent](
      mongoComponent = mongo,
      "event",
      PublishedEvent.mongoEventFormat,
      indexes = Seq(
        IndexModel(ascending("eventId"), IndexOptions().unique(true)),
        IndexModel(
          ascending("createdAt"),
          IndexOptions().name("createdAtTtlIndex").expireAfter(expireAfterSecondsTTL, TimeUnit.SECONDS)
        )
      ),
      replaceIndexes = true
    )
    repository
  }
}
