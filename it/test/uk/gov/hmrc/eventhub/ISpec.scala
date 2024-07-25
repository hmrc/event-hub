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

package uk.gov.hmrc.eventhub

import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, SuiteMixin, TestSuite}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Logger}
import play.api.libs.ws.WSClient
import uk.gov.hmrc.eventhub.UrlHelper.-/
import uk.gov.hmrc.eventhub.modules.MongoSetup
import uk.gov.hmrc.mongo.MongoComponent

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.concurrent.ExecutionContext

trait ISpec extends PlaySpec with ServiceSpec with BeforeAndAfterEach with IntegrationPatience {
  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val mongoSetup = app.injector.instanceOf[MongoSetup]
  val mongoComponent = app.injector.instanceOf[MongoComponent]

  override def afterAll(): Unit = {
    super.afterAll()
  }
}

trait ServiceSpec
    extends SuiteMixin with BeforeAndAfterAll with ScalaFutures with IntegrationPatience with GuiceOneServerPerSuite {
  this: TestSuite =>

  private val logger = Logger(getClass)

  override def fakeApplication(): Application = {
    logger.info(s"""Starting application with additional config:
                   |  ${configMap.mkString("\n  ")}""".stripMargin)
    GuiceApplicationBuilder()
      .configure(configMap)
      .build()
  }

  def additionalConfig: Map[String, ? <: Any] =
    Map.empty

  def testName: String =
    getClass.getSimpleName

  protected val testId =
    TestId(testName)

  protected def serviceMongoUri =
    s"mongodb://localhost:27017/${testId.toString}"

  private lazy val mongoConfig =
    Map(s"mongodb.uri" -> serviceMongoUri)

  private lazy val configMap =
    mongoConfig ++
      additionalConfig

  def resource(path: String): String =
    s"http://localhost:$port/${-/(path)}"

  override def beforeAll(): Unit =
    super.beforeAll()

  override def afterAll(): Unit =
    super.afterAll()
}

object UrlHelper {
  def -/(uri: String) =
    if (uri.startsWith("/")) uri.drop(1) else uri
}

case class TestId(testName: String) {

  val runId =
    DateTimeFormatter.ofPattern("HHmmssSSS").format(LocalDateTime.now())

  override val toString =
    s"${testName.toLowerCase.take(30)}-$runId"
}
