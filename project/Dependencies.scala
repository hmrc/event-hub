/*
 * Copyright 2020 HM Revenue & Customs
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

import play.sbt.PlayImport.ws
import sbt._

object Dependencies {

  private val BOOTSTRAP_VERSION: String = "9.11.0"
  private val MONGO_VERSION: String      = "2.6.0"
  
  object Library {
    val Bootstrap = "uk.gov.hmrc"               %% "bootstrap-backend-play-30"         % BOOTSTRAP_VERSION
    val HmrcMongoWorkItem = "uk.gov.hmrc.mongo" %% "hmrc-mongo-work-item-repo-play-30" % MONGO_VERSION
    val Pureconfig = "com.github.pureconfig"    %% "pureconfig-generic-scala3"         % "0.17.7"
    val Cats = "org.typelevel"                  %% "cats-core"                         % "2.9.0"
    val Swagger = "org.webjars"                  % "swagger-ui"                        % "5.2.0"
    val Enumeration = "com.beachape"            %% "enumeratum-play"                   % "1.8.0"
    val jayway = "com.jayway.jsonpath"           % "json-path"                         % "2.9.0"
    val BootstrapTest = "uk.gov.hmrc"           %% "bootstrap-test-play-30"            % BOOTSTRAP_VERSION
    val HmrcMongoTest = "uk.gov.hmrc.mongo"     %% "hmrc-mongo-test-play-30"           % MONGO_VERSION
    val ScalaCheck = "org.scalatestplus"        %% "scalacheck-1-17"                   % "3.2.18.0"
    val Mockito = "org.scalatestplus"           %% "mockito-4-11"                      % "3.2.17.0"
    val PekkoTestKit = "org.apache.pekko"       %% "pekko-testkit"                     % "1.0.3"
  }

  import Library._

  val libraries = Seq(
    ws,
    Bootstrap,
    HmrcMongoWorkItem,
    Pureconfig,
    Cats,
    Swagger,
    Enumeration,
    jayway,
    BootstrapTest % Test,
    HmrcMongoTest % Test,
    Mockito       % Test,
    PekkoTestKit  % Test,
    ScalaCheck    % Test
  )
}
