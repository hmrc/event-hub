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

import org.apache.pekko.http.scaladsl.model.{HttpMethod, Uri}
import cats.syntax.option._
import cats.syntax.either._
import cats.syntax.parallel._
import com.jayway.jsonpath.JsonPath
import com.typesafe.config.{Config, ConfigObject, ConfigValue}
import pureconfig.ConfigReader
import pureconfig.ConfigReader._
import pureconfig.error.{ConfigReaderFailures, ConvertFailure, FailureReason, UnknownKey}
import uk.gov.hmrc.eventhub.config.ConfigReaders._

import scala.jdk.CollectionConverters._
import scala.concurrent.duration.FiniteDuration
import scala.util.matching.Regex

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

  val topicNamePattern: Regex = "[0-9a-z-]+".r
  val subscriberNamePattern: Regex = "[0-9a-z-]+".r

  final case class InvalidTopicName(topicName: TopicName) extends FailureReason {
    def description: String = s"Invalid topic name: ${topicName.name}"
  }

  final case class InvalidSubscriberName(subscriberName: String) extends FailureReason {
    def description: String = s"Invalid subscriber name: $subscriberName"
  }

  def validateTopicName(topicName: TopicName): Either[FailureReason, TopicName] =
    topicNamePattern.pattern.matcher(topicName.name).matches match {
      case true  => Right(topicName)
      case false => Left(InvalidTopicName(topicName))
    }

  def validateSubscriberName(subscriberName: String): Either[FailureReason, String] =
    subscriberNamePattern.pattern.matcher(subscriberName).matches match {
      case true  => Right(subscriberName)
      case false => Left(InvalidSubscriberName(subscriberName))
    }

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
    topicName: TopicName,
    configValue: ConfigValue,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, List[Subscriber]] =
    validateTopicName(topicName) match {
      case Right(_) =>
        configObjectConfigReader
          .from(configValue)
          .flatMap(subscribersFromConfigObject(topicName, _, subscriptionDefaults))
      case Left(failureReason) =>
        throw new IllegalArgumentException(s"could not load subscription configuration: ${failureReason.description}")
    }

  private def subscribersFromConfigObject(
    topicName: TopicName,
    configObject: ConfigObject,
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, List[Subscriber]] =
    configObject
      .asScala
      .toList
      .parTraverse(subscriberFromConfig(topicName, _, subscriptionDefaults))

  private def subscriberFromConfig(
    topicName: TopicName,
    configValue: (String, ConfigValue),
    subscriptionDefaults: SubscriptionDefaults
  ): Either[ConfigReaderFailures, Subscriber] =
    configValue match {
      case (subscriberName, configValue) =>
        validateSubscriberName(subscriberName) match {
          case Right(_) =>
            configObjectConfigReader
              .from(configValue)
              .map(_.toConfig)
              .flatMap { config =>
                val parentPath = s"${topicName.name}.$subscriberName"
                (
                  subscriberName.asRight,
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
          case Left(failureReason) =>
            throw new IllegalArgumentException(
              s"could not load subscription configuration: ${failureReason.description}"
            )
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
