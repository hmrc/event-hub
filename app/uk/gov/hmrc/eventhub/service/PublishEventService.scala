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

import play.api.Configuration
import uk.gov.hmrc.eventhub.model._
import uk.gov.hmrc.eventhub.respository.{EventHubRepository, SubscriberQueueRepository}
import uk.gov.hmrc.mongo.MongoComponent

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PublishEventService @Inject()(
  eventHubRepository: EventHubRepository,
  configuration: Configuration,
  mongoComponent: MongoComponent,
  @Named("eventTopics") topics: Map[String, List[Topic]]
)(implicit executionContext: ExecutionContext) {

  val subscriberRepos: Map[Subscriber, SubscriberQueueRepository] = topics.flatMap {
    case (name, t) => t.flatMap {
      _.subscribers.map { subscriber =>
        subscriber -> new SubscriberQueueRepository(name, subscriber, configuration, mongoComponent)(executionContext)
      }
    }
  }

  def processEvent(topic: String, event: Event): Future[PublishStatus] = {
    topics.get(topic) match {
      case None => Future.successful(NoTopics)
      case Some(l) =>
        for {
          _ <- eventHubRepository.saveEvent(event)
          status <- Future.sequence(
          l.flatMap(_.subscribers).map { s =>
            subscriberRepos(s).addSubscriberWorkItems(Seq(SubscriberWorkItem(event)))
          }).map(_ => Published)
        } yield status
    }
  }
}
