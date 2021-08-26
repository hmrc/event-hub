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

import cats.syntax.either._
import net.minidev.json.JSONArray
import play.api.libs.json.Json
import uk.gov.hmrc.eventhub.config.Topic
import uk.gov.hmrc.eventhub.model._
import uk.gov.hmrc.eventhub.modules.MongoSetup

import javax.inject.{Inject, Singleton}

@Singleton
class SubscriptionMatcherImpl @Inject() (mongoSetup: MongoSetup) extends SubscriptionMatcher {
  override def apply(event: Event, topic: String): Either[PublishError, Set[SubscriberRepository]] =
    findTopic(topic)
      .map(subscriberRepositories)
      .map(matchingSubscribers(event, _))
      .flatMap {
        case Nil         => NoSubscribersForTopic("No subscribers for topic").asLeft
        case subscribers => subscribers.toSet.asRight
      }

  private def findTopic(topic: String): Either[PublishError, Topic] =
    Either
      .fromOption(
        mongoSetup
          .topics
          .find(_.name == topic),
        NoEventTopic("No such topic")
      )

  private def subscriberRepositories(topic: Topic): List[SubscriberRepository] = {
    val subscriberRepositories = mongoSetup.subscriberRepositories
    topic
      .subscribers
      .map(_.name)
      .flatMap(name => subscriberRepositories.find(_.subscriber.name == name))
  }

  private def matchingSubscribers(
    event: Event,
    subscriberRepositories: List[SubscriberRepository]
  ): List[SubscriberRepository] =
    subscriberRepositories.collect {
      case subscriberRepository
          if subscriberRepository
            .subscriber
            .pathFilter
            .forall(p => !p.read[JSONArray](Json.toJson(event).toString()).isEmpty) =>
        subscriberRepository
    }
}
