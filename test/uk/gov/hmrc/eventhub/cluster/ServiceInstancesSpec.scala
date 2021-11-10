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

package uk.gov.hmrc.eventhub.cluster

import akka.actor.ActorSystem
import org.bson.types.ObjectId
import org.mockito.MockitoSugar.mock
import org.mongodb.scala.model.{Filters, Updates}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.Eventually.eventually
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.eventhub.config.ServiceInstancesConfig
import uk.gov.hmrc.eventhub.metric.MetricsReporter
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.test.CleanMongoCollectionSupport

import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ServiceInstancesSpec
    extends AnyFlatSpec with CleanMongoCollectionSupport with Matchers with BeforeAndAfterEach
    with MongoJavatimeFormats {

  behavior of "ServiceInstances"

  it should "register this instance" in new Scope {
    oneSecond {
      serviceInstances.instanceCount shouldBe 1
    }
  }

  it should "register this instance and observe another" in new Scope {
    instance()

    oneSecond {
      serviceInstances.instanceCount shouldBe 2
    }
  }

  it should "register this instance and observe another being de-registered" in new Scope {
    val id: ObjectId = ObjectId.get()
    val now: Instant = Instant.now()

    serviceInstanceRepository
      .collection
      .insertOne(ServiceInstance(id, now))
      .toFuture()
      .futureValue
      .wasAcknowledged() shouldBe true

    oneSecond {
      serviceInstances.instanceCount shouldBe 2
    }

    oneMinute {
      serviceInstances.instanceCount shouldBe 1
    }
  }

  trait Scope {
    def oneSecond[T](fun: => T): T = eventually(timeout(1.second), interval(100.milliseconds))(fun)
    def oneMinute[T](fun: => T): T = eventually(timeout(1.minute), interval(500.milliseconds))(fun)

    val serviceInstancesConfig: ServiceInstancesConfig = ServiceInstancesConfig(
      timeout = 1.second,
      heartBeatInterval = 300.millis
    )

    private val actorSystem = ActorSystem("ServiceInstancesSpec")
    val metricsReporter: MetricsReporter = mock[MetricsReporter]
    val serviceInstanceRepository = new ServiceInstanceRepository(mongoComponent)
    val serviceInstances: ServiceInstances = instance()

    def instance(): ServiceInstances =
      new ServiceInstances(serviceInstancesConfig, metricsReporter)(
        serviceInstanceRepository,
        actorSystem.scheduler,
        scala.concurrent.ExecutionContext.global
      )
  }
}
