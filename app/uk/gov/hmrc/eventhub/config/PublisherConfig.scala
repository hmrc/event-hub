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

import com.typesafe.config.Config
import play.api.ConfigLoader
import pureconfig.{ConfigConvert, ConfigSource}
import pureconfig.generic.semiauto.*

case class PublisherConfig(transactionRetries: Int)

object PublisherConfig {
  implicit val configConvert: ConfigConvert[PublisherConfig] = deriveConvert[PublisherConfig]
  implicit val configLoader: ConfigLoader[PublisherConfig] = (rootConfig: Config, path: String) =>
    ConfigSource.fromConfig(rootConfig.getConfig(path)).load[PublisherConfig] match {
      case Left(value) =>
        throw new IllegalArgumentException(s"could not load publisher config: ${value.prettyPrint()}")
      case Right(value) => value
    }
}
