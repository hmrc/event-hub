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
import cats.syntax.option._
import cats.syntax.either._
import cats.syntax.parallel._
import com.jayway.jsonpath.JsonPath
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
  maxRetries: Int,
  pathFilter: Option[JsonPath]
)

object Subscriber {
  private implicit class ConfigOps(val config: Config) extends AnyVal {
    def resultValue(path: String, parentPath: String): Result[ConfigValue] = {
      val qualifiedPath = s"$parentPath.$path"
      Either
        .catchNonFatal(config.getValue(path))
        .leftMap(_ => ConfigReaderFailures(new ConvertFailure(UnknownKey(qualifiedPath), None, qualifiedPath)))
    }

    def readOrDefault[T](path: String, parentPath: String, configReader: ConfigReader[T], default: T): Result[T] = {
      val qualifiedPath = s"$parentPath.$path"
      if (config.hasPath(path)) {
        config
          .resultValue(path, parentPath)
          .flatMap(configReader.from)
          .leftMap(qualifyConvertFailure(_, qualifiedPath))
      } else {
        default.asRight
      }
    }

    private def qualifyConvertFailure(
      configReaderFailures: ConfigReaderFailures,
      qualifiedPath: String
    ): ConfigReaderFailures = configReaderFailures match {
      case c: ConfigReaderFailures =>
        c.head match {
          case fail: ConvertFailure => ConfigReaderFailures(new ConvertFailure(fail.reason, fail.origin, qualifiedPath))
          case _                    => c
        }
    }
  }

  def subscribersFromConfig(
    topicName: String,
    configValue: ConfigValue,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, List[Subscriber]] =
    configObjectConfigReader
      .from(configValue)
      .flatMap(subscribersFromConfigObject(topicName, _, subscriptionDefaults))

  private def subscribersFromConfigObject(
    topicName: String,
    configObject: ConfigObject,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, List[Subscriber]] =
    configObject
      .asScala
      .toList
      .parTraverse(subscriberFromConfig(topicName, _, subscriptionDefaults))

  private def subscriberFromConfig(
    topicName: String,
    configValue: (String, ConfigValue),
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, Subscriber] =
    configValue match {
      case (name, configValue) =>
        configObjectConfigReader
          .from(configValue)
          .map(_.toConfig)
          .flatMap { config =>
            val parentPath = s"$topicName.$name"
            (
              name.asRight,
              readUri(config, parentPath),
              readHttpMethod(config, parentPath),
              readElements(config, parentPath, subscriptionDefaults),
              readElementsPer(config, parentPath, subscriptionDefaults),
              readMaxConnections(config, parentPath, subscriptionDefaults),
              readMinBackOff(config, parentPath, subscriptionDefaults),
              readMaxBackOff(config, parentPath, subscriptionDefaults),
              readMaxRetries(config, parentPath, subscriptionDefaults),
              pathFilter(config, parentPath)
            ).parMapN(Subscriber.apply)
          }
    }

  def readUri(config: Config, parentPath: String): Result[Uri] =
    config
      .resultValue(path = "uri", parentPath = parentPath)
      .flatMap(uriReader.from)

  def readHttpMethod(config: Config, parentPath: String): Result[HttpMethod] =
    config.readOrDefault(
      path = "http-method",
      parentPath = parentPath,
      configReader = httpMethodReader,
      default = SubscriptionDefaults.DefaultHttpMethod
    )

  def readElements(
    config: Config,
    parentPath: String,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, Int] =
    config.readOrDefault(
      path = "elements",
      parentPath = parentPath,
      configReader = intConfigReader,
      default = subscriptionDefaults.elements
    )

  def readElementsPer(
    config: Config,
    parentPath: String,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, FiniteDuration] =
    config.readOrDefault(
      path = "per",
      parentPath = parentPath,
      configReader = finiteDurationConfigReader,
      default = subscriptionDefaults.per
    )

  def readMaxConnections(
    config: Config,
    parentPath: String,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, Int] =
    config.readOrDefault(
      path = "max-connections",
      parentPath = parentPath,
      configReader = intConfigReader,
      default = subscriptionDefaults.maxConnections
    )

  def readMinBackOff(
    config: Config,
    parentPath: String,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, FiniteDuration] =
    config.readOrDefault(
      path = "min-back-off",
      parentPath = parentPath,
      configReader = finiteDurationConfigReader,
      default = subscriptionDefaults.minBackOff
    )

  def readMaxBackOff(
    config: Config,
    parentPath: String,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, FiniteDuration] =
    config.readOrDefault(
      path = "max-back-off",
      parentPath = parentPath,
      configReader = finiteDurationConfigReader,
      default = subscriptionDefaults.maxBackOff
    )

  def readMaxRetries(
    config: Config,
    parentPath: String,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, Int] =
    config.readOrDefault(
      path = "max-retries",
      parentPath = parentPath,
      configReader = intConfigReader,
      default = subscriptionDefaults.maxRetries
    )

  def pathFilter(config: Config, parentPath: String): Either[ConfigReaderFailures, Option[JsonPath]] =
    config.readOrDefault(
      path = "filter-path",
      parentPath = parentPath,
      configReader = jsonPathReader.map(_.some),
      default = none[JsonPath]
    )
}
