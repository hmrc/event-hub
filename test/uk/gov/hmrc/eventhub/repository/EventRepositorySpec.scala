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

import com.mongodb.client.result.InsertOneResult
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.when
import org.mongodb.scala.{ClientSession, FindObservable, MongoCollection, Observable, SingleObservable}
import org.mongodb.scala.bson.BsonObjectId
import org.mongodb.scala.bson.conversions.Bson
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.eventhub.model.{Event, PublishedEvent}
import uk.gov.hmrc.eventhub.model.TestModels.event
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.util.UUID

class EventRepositorySpec extends AnyFlatSpec with Matchers with MockitoSugar with ScalaFutures {

  behavior of "EventRepository.addEvent"

  it should "return a mongo InsertOneResult when the underlying repo adds a new event to the event repo" in new Scope {
    when(mongoCollectionMock.insertOne(any(), any[PublishedEvent])).thenReturn(SingleObservable(insertOneResult))
    eventRepository.addEvent(clientSessionMock, event).futureValue shouldBe insertOneResult
  }

  behavior of "EventRepository.find"

  it should "return an event when the underlying repo successfully finds one for that event ID" in new Scope {
    when(findObservableMock.map[Event](any())).thenReturn(Observable(Seq(event)))
    when(mongoCollectionMock.find[PublishedEvent](any[Bson])(any, any)).thenReturn(findObservableMock)
    eventRepository.find(UUID.randomUUID()).futureValue should be(Some(event))
  }

  trait Scope {
    val playMongoEventRepository: PlayMongoRepository[PublishedEvent] = mock[PlayMongoRepository[PublishedEvent]]
    val clientSessionMock: ClientSession = mock[ClientSession]
    val findObservableMock: FindObservable[PublishedEvent] = mock[FindObservable[PublishedEvent]]
    val mongoCollectionMock: MongoCollection[PublishedEvent] = mock[MongoCollection[PublishedEvent]]
    val eventRepository: EventRepository = new EventRepository(playMongoEventRepository)
    val insertOneResult: InsertOneResult = InsertOneResult.acknowledged(BsonObjectId())

    when(playMongoEventRepository.collection).thenReturn(mongoCollectionMock)
  }
}
