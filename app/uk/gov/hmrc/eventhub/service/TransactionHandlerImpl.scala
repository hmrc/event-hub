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

import org.mongodb.scala.{ClientSession, ClientSessionOptions, MongoException, ToSingleObservableVoid, TransactionOptions}
import play.api.i18n.Lang.logger
import uk.gov.hmrc.eventhub.config.PublisherConfig
import uk.gov.hmrc.eventhub.service.TransactionHandler.{TransientTransactionError, UnknownTransactionError}
import uk.gov.hmrc.eventhub.utils.TransactionConfiguration.sessionOptions
import uk.gov.hmrc.mongo.MongoComponent

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TransactionHandlerImpl @Inject() (
  mongoComponent: MongoComponent,
  publisherConfig: PublisherConfig
)(implicit executionContext: ExecutionContext) extends TransactionHandler {
  override def startTransactionSession(
    clientSessionOptions: ClientSessionOptions,
    transactionOptions: TransactionOptions
  ): Future[ClientSession] =
    for {
      clientSession <- mongoComponent.client.startSession(sessionOptions).toFuture()
      _ = Future(clientSession.startTransaction(transactionOptions))
    } yield clientSession

  override def commit(clientSession: ClientSession): Future[Unit] =
    commitWithRetry(clientSession, retryAttempt = 0)

  private def commitWithRetry(clientSession: ClientSession, retryAttempt: Int): Future[Unit] =
    clientSession
      .commitTransaction()
      .map(_ => ())
      .toFuture()
      .map(_ => ())
      .recoverWith(recovery(clientSession, retryAttempt))


  private def recovery(
    clientSession: ClientSession,
    retryAttempt: Int
  ): PartialFunction[Throwable, Future[Unit]] = {
    case e: MongoException if e.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL) =>
      logger.error("TransientTransactionError, this could be one-time network glitch so retrying ...")
      if (retryAttempt < publisherConfig.transactionRetries) {
        commitWithRetry(clientSession, retryAttempt + 1)
      } else {
        throw TransientTransactionError(publisherConfig, e)
      }
    case e: MongoException =>
      logger.error("UnknownTransactionCommitResult, retrying commit operation ...")
      throw UnknownTransactionError(e)
  }
}
