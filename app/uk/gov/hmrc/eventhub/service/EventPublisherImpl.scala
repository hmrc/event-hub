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

package uk.gov.hmrc.eventhub.service

import org.mongodb.scala.result.InsertOneResult
import org.mongodb.scala.{ClientSession, SingleObservable}
import uk.gov.hmrc.eventhub.model.{Event, SubscriberRepository}
import uk.gov.hmrc.eventhub.repository.EventRepository
import uk.gov.hmrc.eventhub.utils.TransactionConfiguration.{sessionOptions, transactionOptions}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EventPublisherImpl @Inject() (
  transactionHandler: TransactionHandler,
  eventRepository: EventRepository,
  publishEventAuditor: PublishEventAuditor
)(implicit executionContext: ExecutionContext)
    extends EventPublisher {
  override def apply(
    event: Event,
    targets: Set[SubscriberRepository]
  ): Future[Unit] = {
    val result = for {
      clientSession <- transactionHandler.startTransactionSession(sessionOptions, transactionOptions)
      eventInsert = eventRepository.addEvent(clientSession, event)
      _      <- targets.foldLeft(eventInsert)(sequenceInserts(clientSession, event))
      committed <- transactionHandler.commit(clientSession)
    } yield committed

    result
      .recover { case exception: Exception =>
        publishEventAuditor.failed(event, exception)
        throw exception
      }
  }

  private def sequenceInserts(
    clientSession: ClientSession,
    event: Event
  ): (Future[InsertOneResult], SubscriberRepository) => Future[InsertOneResult] = {
    case (acc, repository) =>
      acc.flatMap { _ =>
        repository.insertOne(clientSession, event)
      }
  }
}
