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

package uk.gov.hmrc.eventhub.repository

import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.result.InsertOneResult
import org.mongodb.scala.{ClientSession, SingleObservable}
import uk.gov.hmrc.eventhub.model.Event
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EventRepository @Inject()()(implicit ec: ExecutionContext) {

  def addEvent(cs: ClientSession, eventRepository: PlayMongoRepository[Event], event: Event): SingleObservable[InsertOneResult] =
    eventRepository.collection.insertOne(cs, event)

  def find(eventId: String, eventRepository: PlayMongoRepository[Event]): Future[Seq[Event]] =
    eventRepository.collection.find(equal("eventId", eventId)).toFuture()
}
