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

import org.mongodb.scala.ClientSession
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.result.InsertOneResult
import org.mongodb.scala.SingleObservableFuture
import uk.gov.hmrc.eventhub.model.{Event, PublishedEvent}
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class EventRepository @Inject() (eventRepository: PlayMongoRepository[PublishedEvent]) {

  def addEvent(cs: ClientSession, event: Event): Future[InsertOneResult] =
    eventRepository.collection.insertOne(cs, PublishedEvent.from(event)).toFuture()

  def find(eventId: UUID): Future[Option[Event]] =
    eventRepository
      .collection
      .find(equal("eventId", eventId.toString))
      .map(e => PublishedEvent.to(e))
      .headOption()
}
