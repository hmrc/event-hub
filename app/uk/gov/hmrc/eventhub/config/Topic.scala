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

package uk.gov.hmrc.eventhub.config

import cats.syntax.parallel._
import com.typesafe.config.{Config, ConfigValue}
import play.api.ConfigLoader
import pureconfig.error.ConfigReaderFailures

import scala.collection.JavaConverters._

case class Topic(name: String, subscribers: List[Subscriber])

object Topic {
  def configLoader(subscriptionDefaults: SubscriptionDefaults): ConfigLoader[Set[Topic]] =
    (rootConfig: Config, path: String) =>
      rootConfig
        .getObject(path)
        .asScala
        .toList
        .parTraverse(topicsFromConfig(_, subscriptionDefaults)) match {
        case Right(topics) => topics.toSet
        case Left(configReaderFailures) =>
          throw new IllegalArgumentException(configReaderFailures.prettyPrint())
      }

  private def topicsFromConfig(
    configValue: (String, ConfigValue),
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, Topic] = configValue match {
    case (topicName, subscriberList) =>
      Subscriber
        .subscribersFromConfig(topicName, subscriberList, subscriptionDefaults)
        .map(Topic(topicName, _))
  }
}
