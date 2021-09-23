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

package uk.gov.hmrc.eventhub

import org.mongodb.scala.{MongoClient, MongoDatabase}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.WSClient
import uk.gov.hmrc.eventhub.modules.MongoSetup
import uk.gov.hmrc.integration.ServiceSpec
import uk.gov.hmrc.mongo.MongoComponent

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

trait ISpec extends PlaySpec with ServiceSpec with BeforeAndAfterEach with IntegrationPatience {
  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val mongoSetup = app.injector.instanceOf[MongoSetup]
  val mongoComponent = app.injector.instanceOf[MongoComponent]

  override def afterAll(): Unit = {
    implicit val patienceConfig: PatienceConfig =
      PatienceConfig(
        timeout = scaled(Span(60, Seconds)),
        interval = scaled(Span(150, Millis))
      )
    super.afterAll()
    val mongoClient: MongoClient = MongoClient(s"mongodb://mongo:27017/$testName?replicaSet=devrs")
    val mongoDatabase: MongoDatabase = mongoClient.getDatabase(testName)
//    mongoDatabase.drop().toFuture().futureValue
  }
}
