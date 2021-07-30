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

package uk.gov.hmrc.eventhub.model

import akka.http.scaladsl.model.{ HttpMethod, Uri }
import cats.kernel.Semigroup
import cats.syntax.either._
import cats.instances.either._
import cats.syntax.parallel._
import cats.instances.list._
import com.typesafe.config.{ Config, ConfigObject, ConfigValue }
import play.api.ConfigLoader
import pureconfig.ConfigReader._
import pureconfig.error.ConfigReaderFailures
import uk.gov.hmrc.eventhub.config.ConfigReaders._
import uk.gov.hmrc.eventhub.config.SubscriptionDefaults

import scala.collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

case class Subscriber(
    name: String,
    uri: Uri,
    httpMethod: HttpMethod,
    elements: Int,
    per: FiniteDuration,
    maxConnections: Int,
    minBackOff: FiniteDuration,
    maxBackOff: FiniteDuration,
    maxRetries: Int
)

object Subscriber {
  implicit object semigroupConfigReaderFailures
      extends Semigroup[ConfigReaderFailures] {
    override def combine(x: ConfigReaderFailures,
                         y: ConfigReaderFailures): ConfigReaderFailures =
      x.++(y)
  }

  def configLoader(
      subscriptionDefaults: SubscriptionDefaults): ConfigLoader[Set[Topic]] =
    (rootConfig: Config, path: String) =>
      rootConfig
        .getObject(path)
        .asScala
        .toList
        .parTraverse(topicConfig(_, subscriptionDefaults)) match {
        case Right(topics) => topics.toSet
        case Left(configReaderFailures) =>
          throw new IllegalArgumentException(configReaderFailures.prettyPrint())
    }

  private def topicConfig(
      configValue: (String, ConfigValue),
      subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, Topic] = configValue match {
    case (name, subscriberList) =>
      subscribersConfig(subscriberList, subscriptionDefaults).map(
        Topic(name, _))
  }

  private def subscribersConfig(configValue: ConfigValue,
                                subscriptionDefaults: SubscriptionDefaults)
    : Either[ConfigReaderFailures, List[Subscriber]] =
    configObjectConfigReader
      .from(configValue)
      .flatMap(subscribersFromConfigObject(_, subscriptionDefaults))

  private def subscribersFromConfigObject(
      configObject: ConfigObject,
      subscriptionDefaults: SubscriptionDefaults)
    : Either[ConfigReaderFailures, List[Subscriber]] =
    configObject.asScala.toList
      .parTraverse(subscriberConfig(_, subscriptionDefaults))

  private def subscriberConfig(configValue: (String, ConfigValue),
                               subscriptionDefaults: SubscriptionDefaults)
    : Either[ConfigReaderFailures, Subscriber] =
    configValue match {
      case (name, configValue) =>
        configObjectConfigReader
          .from(configValue)
          .map(_.toConfig)
          .flatMap { config =>
            (
              name.asRight,
              uriReader
                .from(config.getValue("uri")),
              httpMethodReader
                .from(config.getValue("http-method"))
                .orElse(subscriptionDefaults.httpMethod.asRight),
              intConfigReader
                .from(config.getValue("elements"))
                .orElse(subscriptionDefaults.elements.asRight),
              finiteDurationConfigReader
                .from(config.getValue("per"))
                .orElse(subscriptionDefaults.per.asRight),
              intConfigReader
                .from(config.getValue("max-connections"))
                .orElse(subscriptionDefaults.maxConnections.asRight),
              finiteDurationConfigReader
                .from(config.getValue("min-back-off"))
                .orElse(subscriptionDefaults.minBackOff.asRight),
              finiteDurationConfigReader
                .from(config.getValue("max-back-off"))
                .orElse(subscriptionDefaults.maxBackOff.asRight),
              intConfigReader
                .from(config.getValue("max-retries"))
                .orElse(subscriptionDefaults.maxRetries.asRight)
            ).parMapN(Subscriber.apply)
          }
    }
}
