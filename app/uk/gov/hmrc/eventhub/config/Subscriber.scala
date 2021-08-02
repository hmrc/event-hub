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
import pureconfig.ConfigReader
import pureconfig.ConfigReader._
import pureconfig.error.{ConfigReaderFailures, ConvertFailure, UnknownKey}
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
  private implicit class ConfigOps(val config: Config) extends AnyVal {
    def resultValue(path: String, parentPath: String): Result[ConfigValue] = {
      val qualifiedPath = s"$parentPath.$path"
      Either
        .catchNonFatal(config.getValue(path))
        .leftMap(_ => ConfigReaderFailures(new ConvertFailure(UnknownKey(qualifiedPath), None, qualifiedPath)))
    }

    def readOrDefault[T](path: String, parentPath: String, configReader: ConfigReader[T], default: T): Result[T] =
      if (config.hasPath(path)) {
        config.resultValue(path, parentPath).flatMap(configReader.from)
      } else {
        default.asRight
      }
  }

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
              readUri(config, name),
              readHttpMethod(config, name),
              readElements(config, name, subscriptionDefaults),
              readElementsPer(config, name, subscriptionDefaults),
              readMaxConnections(config, name, subscriptionDefaults),
              readMinBackOff(config, name, subscriptionDefaults),
              readMaxBackOff(config, name, subscriptionDefaults),
              readMaxRetries(config, name, subscriptionDefaults)
            ).parMapN(Subscriber.apply)
          }
    }

  def readUri(config: Config, name: String): Result[Uri] =
    config
      .resultValue(path = "uri", parentPath = name)
      .flatMap(uriReader.from)

  def readHttpMethod(config: Config, name: String): Result[HttpMethod] =
    config.readOrDefault(
      path = "http-method",
      parentPath = name,
      configReader = httpMethodReader,
      default = SubscriptionDefaults.DefaultHttpMethod
    )

  def readElements(config: Config, name: String, subscriptionDefaults: SubscriptionDefaults): Either[ConfigReaderFailures, Int] =
    config.readOrDefault(
      path = "elements",
      parentPath = name,
      configReader = intConfigReader,
      default = subscriptionDefaults.elements
    )

  def readElementsPer(
    config: Config,
    name: String,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, FiniteDuration] =
    config.readOrDefault(
      path = "per",
      parentPath = name,
      configReader = finiteDurationConfigReader,
      default = subscriptionDefaults.per
    )

  def readMaxConnections(
    config: Config,
    name: String,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, Int] =
    config.readOrDefault(
      path = "max-connections",
      parentPath = name,
      configReader = intConfigReader,
      default = subscriptionDefaults.maxConnections
    )

  def readMinBackOff(
    config: Config,
    name: String,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, FiniteDuration] =
    config.readOrDefault(
      path = "min-back-off",
      parentPath = name,
      configReader = finiteDurationConfigReader,
      default = subscriptionDefaults.minBackOff
    )

  def readMaxBackOff(
    config: Config,
    name: String,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, FiniteDuration] =
    config.readOrDefault(
      path = "max-back-off",
      parentPath = name,
      configReader = finiteDurationConfigReader,
      default = subscriptionDefaults.maxBackOff
    )

  def readMaxRetries(config: Config, name: String, subscriptionDefaults: SubscriptionDefaults): Either[ConfigReaderFailures, Int] =
    config.readOrDefault(
      path = "max-retries",
      parentPath = name,
      configReader = intConfigReader,
      default = subscriptionDefaults.maxRetries
    )
}
