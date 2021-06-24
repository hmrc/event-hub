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

import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.{ClientSession, MongoException, Observable, ToSingleObservableVoid}
import play.api.i18n.Lang.logger
import uk.gov.hmrc.eventhub.models._
import uk.gov.hmrc.eventhub.modules.MongoSetup
import uk.gov.hmrc.eventhub.repository.{EventRepository, SubscriberQueuesRepository}
import uk.gov.hmrc.eventhub.utils.HelperFunctions.liftFuture
import uk.gov.hmrc.eventhub.utils.TransactionConfiguration.{sessionOptions, transactionOptions}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem, WorkItemRepository}
import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PublisherService @Inject()( mongoComponent: MongoComponent,
                                  eventRepository: EventRepository,
                                  subscriberQueuesRepository: SubscriberQueuesRepository,
                                  mongoSetup: MongoSetup

   )(implicit ec: ExecutionContext) {

 private def commitAndRetry(clientSession: ClientSession): Observable[Unit] = {
    clientSession
      .commitTransaction()
      .map(_ => ())
      .recoverWith(recovery(clientSession))
      .map(_ => clientSession.close())
  }

  private def recovery(clientSession: ClientSession): PartialFunction[Throwable, Observable[Unit]] = {
    case e: MongoException if e.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL) =>
      logger.error("TransientTransactionError, this could be one-time network glitch so retrying ...")
      commitAndRetry(clientSession)
    case e: MongoException  =>
      logger.error("UnknownTransactionCommitResult, retrying commit operation ...")
    throw new Exception(s"Unknown Transaction error $e")
  }

  def publishIfUnique(topic: String, event: Event): Future[Either[PublishError, Unit]] = {
    eventRepository.find(event.eventId.toString, mongoSetup.eventRepository).map(_.isEmpty) flatMap   {
      case true => liftFuture(publishIfValid(topic, event))
      case _ => Future.successful(Left(DuplicateEvent("Duplicate Event: Event with eventId already exists")))
    }
  }

 private def publishIfValid(topic: String, event: Event): Either[PublishError, Future[Unit]] = {
   mongoSetup.topics.get(topic) match {
     case None => Left(NoEventTopic("No such topic"))
     case Some(subscribers) if subscribers.isEmpty => Left(NoSubscribersForTopic("No subscribers for topic"))
     case Some(_) => Right(publish(event, subscriberRepos(topic)))
   }
}

 private def subscriberRepos(topic: String) = mongoSetup.subscriberRepositories.filter(_._1 == topic).map(_._2)

 private def publish(event: Event, subscriberRepos: Seq[WorkItemRepository[Event]]): Future[Unit] = {
        mongoComponent.client.startSession(sessionOptions).flatMap(clientSession => {
          clientSession.startTransaction(transactionOptions)
          val eventInsert = eventRepository.addEvent(clientSession, mongoSetup.eventRepository, event)

          val sequenced = subscriberRepos.foldLeft(eventInsert) {
            (acc, repository) =>
              acc.flatMap { _ =>
               subscriberQueuesRepository.addWorkItem(clientSession, repository, subscriberWorkItem(event))
              }
          }
          sequenced.map(_ => clientSession)
        })
      }.flatMap(commitAndRetry).head()


  private def subscriberWorkItem(event: Event): WorkItem[Event] =
    WorkItem(
      id = new ObjectId(),
      receivedAt = Instant.now(),
      updatedAt = Instant.now(),
      availableAt = Instant.now(),
      status = ProcessingStatus.ToDo,
      failureCount = 0,
      item = event
    )
}
