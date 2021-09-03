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

import com.mongodb.MongoException
import org.mongodb.scala.{ClientSession, ClientSessionOptions, TransactionOptions}
import play.api.Logging
import uk.gov.hmrc.eventhub.config.PublisherConfig
import uk.gov.hmrc.eventhub.config.TransactionConfiguration.sessionOptions
import uk.gov.hmrc.eventhub.service.TransactionHandler.TransientTransactionError
import uk.gov.hmrc.mongo.MongoComponent

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TransientTransactionThrowingHandler @Inject() (
  mongoComponent: MongoComponent,
  publisherConfig: PublisherConfig
)(implicit executionContext: ExecutionContext)
    extends TransactionHandler with Logging {
  override def startTransactionSession(
    clientSessionOptions: ClientSessionOptions,
    transactionOptions: TransactionOptions
  ): Future[ClientSession] =
    for {
      clientSession <- mongoComponent.client.startSession(sessionOptions).toFuture()
      _ = Future(clientSession.startTransaction(transactionOptions))
    } yield clientSession

  override def commit(clientSession: ClientSession): Future[Unit] = {
    clientSession.abortTransaction()
    clientSession.close()

    logger.info("failing request with transient transaction exception")
    Future.failed(
      TransientTransactionError(publisherConfig, MongoException.fromThrowable(new IllegalStateException("boom")))
    )
  }
}
