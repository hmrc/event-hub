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

import cats.data.OptionT
import cats.syntax.option._
import org.mongodb.scala.bson.ObjectId
import play.api.Logging
import uk.gov.hmrc.eventhub.model.Event
import uk.gov.hmrc.mongo.workitem.ProcessingStatus.PermanentlyFailed
import WorkItemSubscriberEventRepository.oidRegex

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

class WorkItemSubscriberEventRepository(
  subscriberQueueRepository: SubscriberQueueRepository
)(implicit executionContext: ExecutionContext)
    extends SubscriberEventRepository with Logging {
  override def next(): Future[Option[Event]] =
    subscriberQueueRepository
      .getEvent
      .map(_.map(_.item))
      .recoverWith(readRecover)

  private def readRecover: PartialFunction[Throwable, Future[Option[Event]]] = {
    case r: RuntimeException if r.getMessage.contains("Failed to parse json") =>
      logger.error(
        s"failed to deserialize event json, due to:  ${r.getMessage}. Attempting to mark as PermanentlyFailed."
      )

      oidRegex
        .findFirstMatchIn(r.getMessage)
        .flatMap(first => Option(first.group(1))) match {
        case Some(oid) =>
          subscriberQueueRepository.complete(new ObjectId(oid), PermanentlyFailed).flatMap {
            case true => next()
            case _    => throw r
          }
        case _ => throw r
      }
  }

  override def failed(event: Event): Future[Option[Boolean]] =
    (for {
      workItem <- OptionT(subscriberQueueRepository.findAsWorkItem(event))
      result   <- OptionT(subscriberQueueRepository.failed(workItem).map(_.some))
    } yield {
      logger.debug(s"marking $event as failed: $result")
      result
    }).value

  override def sent(event: Event): Future[Option[Boolean]] =
    (for {
      workItem <- OptionT(subscriberQueueRepository.findAsWorkItem(event))
      result   <- OptionT(subscriberQueueRepository.completeAndDelete(workItem.id).map(_.some))
    } yield {
      logger.debug(s"marking $event as sent: $result")
      result
    }).value

  override def remove(event: Event): Future[Option[Boolean]] =
    (for {
      workItem <- OptionT(subscriberQueueRepository.findAsWorkItem(event))
      result   <- OptionT(subscriberQueueRepository.permanentlyFailed(workItem).map(_.some))
    } yield {
      logger.debug(s"removing $event: $result")
      result
    }).value
}

object WorkItemSubscriberEventRepository {
  val oidRegex: Regex = """oid":"([a-z0-9]+)""".r
}
