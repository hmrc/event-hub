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

import akka.http.scaladsl.model.HttpMethods.{POST, PUT}
import akka.http.scaladsl.model.{HttpMethod, HttpMethods, Uri}
import com.jayway.jsonpath.JsonPath
import pureconfig.{ConfigCursor, ConfigReader}

object ConfigReaders {
  implicit val uriReader: ConfigReader[Uri] = (cur: ConfigCursor) => cur.asString.map(s => Uri(s))
  implicit val httpMethodReader: ConfigReader[HttpMethod] = (cur: ConfigCursor) => cur.asString.map(postOrPut)
  implicit val jsonPathReader: ConfigReader[JsonPath] = (cur: ConfigCursor) => cur.asString.map(JsonPath.compile(_))

  private def postOrPut(configValue: String): HttpMethod =
    HttpMethods
      .getForKeyCaseInsensitive(configValue)
      .flatMap(postOrPut)
      .getOrElse(throw new IllegalArgumentException(s"expected one of [POST, PUT], but got: $configValue."))

  private def postOrPut(httpMethod: HttpMethod): Option[HttpMethod] = httpMethod match {
    case postOrPut @ (POST | PUT) => Some(postOrPut)
    case _                        => None
  }
}
