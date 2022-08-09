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

package uk.gov.hmrc.eventhub.service

import org.mongodb.scala.{ClientSession, ClientSessionOptions, MongoException, TransactionOptions}
import play.api.libs.json.Json
import uk.gov.hmrc.eventhub.config.{PublisherConfig, Subscriber, TopicName}
import uk.gov.hmrc.eventhub.model._
import uk.gov.hmrc.play.audit.model.{DataEvent, EventTypes}

import scala.concurrent.Future

trait EventPublisherService {
  def publish(event: Event, topicName: TopicName): Future[Either[PublishError, Set[Subscriber]]]
}

trait SubscriptionMatcher {
  def apply(event: Event, topicName: TopicName): Either[PublishError, Set[SubscriberRepository]]
}

trait EventPublisher {
  def apply(event: Event, targets: Set[SubscriberRepository]): Future[Unit]
}

trait TransactionHandler {
  def startTransactionSession(
    clientSessionOptions: ClientSessionOptions,
    transactionOptions: TransactionOptions
  ): Future[ClientSession]
  def commit(clientSession: ClientSession): Future[Unit]
}

trait PublishEventAuditor {
  def failed(event: Event, exception: Exception): Unit
}

object TransactionHandler {
  sealed abstract class TransactionError(message: String) extends Exception(message)

  case class TransientTransactionError(publisherConfig: PublisherConfig, mongoException: MongoException)
      extends TransactionError(
        s"failed to commit transaction after ${publisherConfig.transactionRetries} retry attempts, due to: ${mongoException.getMessage}"
      )

  case class UnknownTransactionError(mongoException: MongoException)
      extends TransactionError(
        s"failed to commit transaction due to: ${mongoException.getMessage}"
      )
}

object PublishEventAuditor {
  def asDataEvent(event: Event, exception: Exception): DataEvent = {
    val map = Json
      .toJsObject(event)
      .value
      .mapValues(_.toString())
      .toMap + ("reason" -> exception.getMessage)

    DataEvent(
      auditSource = "event-hub-publisher",
      auditType = EventTypes.Failed,
      detail = map
    )
  }
}
