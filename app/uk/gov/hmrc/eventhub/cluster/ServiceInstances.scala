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

package uk.gov.hmrc.eventhub.cluster

import org.apache.pekko.actor.Scheduler
import org.apache.pekko.pattern.Patterns.after
import org.bson.types.ObjectId
import org.mongodb.scala.SingleObservable
import org.mongodb.scala.{ObservableFuture, SingleObservableFuture}
import org.mongodb.scala.ToSingleObservablePublisher
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model._
import org.mongodb.scala.result.UpdateResult
import play.api.Logging
import uk.gov.hmrc.eventhub.config.ServiceInstancesConfig
import uk.gov.hmrc.eventhub.metric.MetricsReporter

import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicLong
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class ServiceInstances @Inject() (serviceInstancesTimeout: ServiceInstancesConfig, metricsReporter: MetricsReporter)(
  implicit
  repo: ServiceInstanceRepository,
  scheduler: Scheduler,
  executionContext: ExecutionContext
) extends Logging {

  private val LastSeenAt = "lastSeenAt"
  private val instanceId = ObjectId.get()
  private val atomicInstanceCount = new AtomicLong(1)
  private val callable: Callable[Future[Long]] = () => heartbeat().toFuture()
  initialise()

  private def initialise() = {
    metricsReporter.gaugeServiceInstances(() => instanceCount)

    Await
      .result(
        repo
          .collection
          .insertOne(ServiceInstance(instanceId, Instant.now()))
          .toFuture(),
        3.seconds
      )
      .wasAcknowledged()

    heartbeat()
    scheduleHeartBeat(retryCount = 0)
  }

  private def scheduleHeartBeat(retryCount: Int): Future[Long] =
    after(serviceInstancesTimeout.heartBeatInterval, scheduler, executionContext, callable)
      .transformWith {
        case Success(_) =>
          logger.debug(s"heart beat success: instance count = $instanceCount")
          scheduleHeartBeat(retryCount = 0)
        case Failure(exception) =>
          logger.error(s"heart beat failure: ${exception.getMessage} - retry attempt: $retryCount")
          scheduleHeartBeat(retryCount + 1)
      }

  def heartbeat(): SingleObservable[Long] = {
    val now = Instant.now()
    for {
      alive  <- keepAlive(now)
      _      <- cleanInactive(now)
      active <- activeInstances(now)
    } yield {
      alive.wasAcknowledged()
      atomicInstanceCount.set(active)
      active
    }
  }

  def instanceCount: Int = atomicInstanceCount.get().toInt

  private def keepAlive(now: Instant): SingleObservable[UpdateResult] =
    repo
      .collection
      .updateOne(equal("_id", instanceId), Updates.set(LastSeenAt, now))

  private def activeInstances(now: Instant): SingleObservable[Long] =
    repo
      .collection
      .countDocuments(Filters.gte(LastSeenAt, now.minusMillis(serviceInstancesTimeout.timeout.toMillis)))

  private def cleanInactive(now: Instant): SingleObservable[Long] =
    repo
      .collection
      .deleteMany(Filters.lt(LastSeenAt, now.minusMillis(serviceInstancesTimeout.timeout.toMillis)))
      .map(_.getDeletedCount)
}
