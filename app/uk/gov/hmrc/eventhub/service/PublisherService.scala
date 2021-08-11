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

import net.minidev.json.JSONArray
import org.mongodb.scala.bson.ObjectId
import org.mongodb.scala.result.InsertOneResult
import org.mongodb.scala.{ClientSession, MongoException, Observable, SingleObservable, ToSingleObservableVoid}
import play.api.i18n.Lang.logger
import play.api.libs.json.Json
import uk.gov.hmrc.eventhub.config.Subscriber
import uk.gov.hmrc.eventhub.model._
import uk.gov.hmrc.eventhub.modules.MongoSetup
import uk.gov.hmrc.eventhub.repository.{EventRepository, SubscriberQueuesRepository}
import uk.gov.hmrc.eventhub.utils.HelperFunctions.liftFuture
import uk.gov.hmrc.eventhub.utils.TransactionConfiguration.{sessionOptions, transactionOptions}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.workitem.{ProcessingStatus, WorkItem, WorkItemRepository}

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PublisherService @Inject() (
  mongoComponent: MongoComponent,
  eventRepository: EventRepository,
  subscriberQueuesRepository: SubscriberQueuesRepository,
  mongoSetup: MongoSetup
)(implicit ec: ExecutionContext) {

  private[service] def commitAndRetry(clientSession: ClientSession): Observable[Unit] =
    clientSession.commitTransaction().map(_ => ()).recoverWith(recovery(clientSession)).map(_ => clientSession.close())

  private def recovery(clientSession: ClientSession): PartialFunction[Throwable, Observable[Unit]] = {
    case e: MongoException if e.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL) =>
      logger.error("TransientTransactionError, this could be one-time network glitch so retrying ...")
      commitAndRetry(clientSession)
    case e: MongoException =>
      logger.error("UnknownTransactionCommitResult, retrying commit operation ...")
      throw new Exception(s"Unknown Transaction error $e")
  }

  private[service] def matchingSubscribers(event: Event, subscribers: List[Subscriber]): immutable.Seq[Subscriber] =
    subscribers.collect {
      case subscriber: Subscriber
          if subscriber
            .pathFilter
            .forall(p => !p.read[JSONArray](Json.toJson(event).toString()).isEmpty) =>
        subscriber
    }

  def publishIfUnique(topic: String, event: Event): Future[Either[PublishError, Unit]] =
    eventRepository.find(event.eventId.toString, mongoSetup.eventRepository).map(_.isEmpty) flatMap {
      case true => liftFuture(publishIfValid(topic, event))
      case _    => Future.successful(Left(DuplicateEvent("Duplicate Event: Event with eventId already exists")))
    }

  private def publishIfValid(topic: String, event: Event): Either[PublishError, Future[Unit]] =
    mongoSetup.topics.find(_.name == topic) match {
      case None                                     => Left(NoEventTopic("No such topic"))
      case Some(topic) if topic.subscribers.isEmpty => Left(NoSubscribersForTopic("No subscribers for topic"))
      case Some(topicObj) =>
        Right(
          publish(
            event,
            subscriberReposFiltered(topic, matchingSubscribers(event, topicObj.subscribers)).map(_.workItemRepository)
          )
        )
    }

  private[service] def subscriberReposFiltered(topic: String, subscribers: Seq[Subscriber]) = {
    val topicRepos = mongoSetup.subscriberRepositories.filter(_.topic == topic)
    topicRepos.filter(x => subscribers.map(_.name).contains(x.subscriber.name))
  }

  private[service] def publish(event: Event, subscriberRepos: Set[WorkItemRepository[Event]]): Future[Unit] =
    mongoComponent
      .client
      .startSession(sessionOptions)
      .flatMap { clientSession =>
        clientSession.startTransaction(transactionOptions)
        val eventInsert: SingleObservable[InsertOneResult] =
          eventRepository.addEvent(clientSession, mongoSetup.eventRepository, event)

        val sequenced: SingleObservable[InsertOneResult] = subscriberRepos.foldLeft(eventInsert) { (acc, repository) =>
          acc.flatMap { _ =>
            subscriberQueuesRepository.addWorkItem(clientSession, repository, subscriberWorkItem(event))
          }
        }
        sequenced.map(_ => clientSession)
      }
      .flatMap(commitAndRetry)
      .head()

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
