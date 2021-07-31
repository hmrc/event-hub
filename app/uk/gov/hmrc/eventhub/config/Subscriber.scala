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

import akka.http.scaladsl.model.{HttpMethod, Uri}
import cats.syntax.either._
import cats.syntax.parallel._
import com.typesafe.config.{Config, ConfigObject, ConfigValue}
import pureconfig.ConfigReader._
import pureconfig.error.ConfigReaderFailures
import uk.gov.hmrc.eventhub.config.ConfigReaders._

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

  def subscribersFromConfig(
    configValue: ConfigValue,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, List[Subscriber]] =
    configObjectConfigReader
      .from(configValue)
      .flatMap(subscribersFromConfigObject(_, subscriptionDefaults))

  private def subscribersFromConfigObject(
    configObject: ConfigObject,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, List[Subscriber]] =
    configObject
      .asScala
      .toList
      .parTraverse(subscriberFromConfig(_, subscriptionDefaults))

  private def subscriberFromConfig(
    configValue: (String, ConfigValue),
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, Subscriber] =
    configValue match {
      case (name, configValue) =>
        configObjectConfigReader
          .from(configValue)
          .map(_.toConfig)
          .flatMap { config =>
            (
              name.asRight,
              readUri(config),
              readHttpMethod(config, subscriptionDefaults),
              readElements(config, subscriptionDefaults),
              readElementsPer(config, subscriptionDefaults),
              readMaxConnections(config, subscriptionDefaults),
              readMinBackOff(config, subscriptionDefaults),
              readMaxBackOff(config, subscriptionDefaults),
              readMaxRetries(config, subscriptionDefaults)
            ).parMapN(Subscriber.apply)
          }
    }

  def readUri(config: Config): Result[Uri] =
    uriReader.from(config.getValue("uri"))

  def readHttpMethod(config: Config, subscriptionDefaults: SubscriptionDefaults): Result[HttpMethod] =
    httpMethodReader
      .from(config.getValue("http-method"))
      .orElse(subscriptionDefaults.httpMethod.asRight)

  def readElements(config: Config, subscriptionDefaults: SubscriptionDefaults): Either[ConfigReaderFailures, Int] =
    intConfigReader
      .from(config.getValue("elements"))
      .orElse(subscriptionDefaults.elements.asRight)

  def readElementsPer(
    config: Config,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, FiniteDuration] =
    finiteDurationConfigReader
      .from(config.getValue("per"))
      .orElse(subscriptionDefaults.per.asRight)

  def readMaxConnections(
    config: Config,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, Int] =
    intConfigReader
      .from(config.getValue("max-connections"))
      .orElse(subscriptionDefaults.maxConnections.asRight)

  def readMinBackOff(
    config: Config,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, FiniteDuration] =
    finiteDurationConfigReader
      .from(config.getValue("min-back-off"))
      .orElse(subscriptionDefaults.minBackOff.asRight)

  def readMaxBackOff(
    config: Config,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, FiniteDuration] =
    finiteDurationConfigReader
      .from(config.getValue("max-back-off"))
      .orElse(subscriptionDefaults.maxBackOff.asRight)

  def readMaxRetries(config: Config, subscriptionDefaults: SubscriptionDefaults): Either[ConfigReaderFailures, Int] =
    intConfigReader
      .from(config.getValue("max-retries"))
      .orElse(subscriptionDefaults.maxRetries.asRight)
}
