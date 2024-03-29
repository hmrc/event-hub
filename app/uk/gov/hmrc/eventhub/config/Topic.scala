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

package uk.gov.hmrc.eventhub.config

import cats.syntax.parallel._
import cats.syntax.either._
import com.typesafe.config.{Config, ConfigValue, ConfigValueType}
import play.api.ConfigLoader
import pureconfig.ConfigReader.configObjectConfigReader
import pureconfig.error.ConfigReaderFailures

import scala.jdk.CollectionConverters._

case class Topic(name: TopicName, subscribers: List[Subscriber])

object Topic {
  def empty(name: TopicName): Topic = Topic(name, Nil)

  def configLoader(subscriptionDefaults: SubscriptionDefaults): ConfigLoader[Set[Topic]] =
    (rootConfig: Config, path: String) =>
      configObjectConfigReader
        .from(rootConfig.getValue(path))
        .map(_.asScala.toList)
        .flatMap(_.parTraverse(topicsFromConfig(_, subscriptionDefaults)))
        .valueOr(error => throw new IllegalArgumentException(error.prettyPrint()))
        .toSet

  private def topicsFromConfig(
    configValue: (String, ConfigValue),
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, Topic] = configValue match {
    case (name, subscriberList) if subscriberList.valueType() == ConfigValueType.STRING =>
      Topic.empty(TopicName(name)).asRight
    case (name, subscriberList) =>
      val topicName = TopicName(name)
      Subscriber
        .subscribersFromConfig(topicName, subscriberList, subscriptionDefaults)
        .map(Topic(topicName, _))
  }
}
