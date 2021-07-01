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

package uk.gov.hmrc.eventhub.respository

import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.ascending
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions}
import org.mongodb.scala.result.{DeleteResult, InsertOneResult}
import play.api.Configuration
import uk.gov.hmrc.eventhub.model.{Event, MongoEvent}
import uk.gov.hmrc.eventhub.respository.EventHubRepository.ExpireAfter
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.{Duration, Instant}
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class EventHubRepository @Inject()(configuration : Configuration, mongo: MongoComponent)(implicit ec: ExecutionContext) extends PlayMongoRepository[MongoEvent](
  mongoComponent = mongo,
  collectionName = "event-hub",
  domainFormat   = MongoEvent.fmt,
  indexes        =  Seq(IndexModel(ascending("createdAt"),IndexOptions().expireAfter(ExpireAfter, TimeUnit.SECONDS)))
){

  private val deleteEventAfter: Duration =
    configuration.underlying.getDuration("queue.deleteEventAfter")

  def saveEvent(event: Event): Future[InsertOneResult] = collection.insertOne(MongoEvent.newMongoEvent(Instant.now, event)).toFuture()

  def findEventByMessageId(messageId: UUID): Future[MongoEvent] =
    collection.find(equal("event.eventId", messageId.toString)).first().toFuture()

  def removeExpiredEvents: Future[DeleteResult] =
    collection.deleteMany(Filters.lt("createdAt", Instant.now.minus(deleteEventAfter))).toFuture()

}

object EventHubRepository {
  val ExpireAfter = 300
}


