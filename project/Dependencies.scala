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

import play.core.PlayVersion.current
import play.sbt.PlayImport.ws
import sbt._

object Dependencies {

  object Library {
    val AkkaHttp = "com.typesafe.akka"          %% "akka-http"                         % "10.2.8"
    val Bootstrap = "uk.gov.hmrc"               %% "bootstrap-backend-play-28"         % "5.4.0"
    val HmrcMongo = "uk.gov.hmrc.mongo"         %% "hmrc-mongo-play-28"                % "0.59.0"
    val HmrcMongoWorkItem = "uk.gov.hmrc.mongo" %% "hmrc-mongo-work-item-repo-play-28" % "0.59.0"
    val Pureconfig = "com.github.pureconfig"    %% "pureconfig"                        % "0.17.1"
    val Cats = "org.typelevel"                  %% "cats-core"                         % "2.7.0"
    val Swagger = "org.webjars"                  % "swagger-ui"                        % "3.50.0"
    val Enumeration = "com.beachape"            %% "enumeratum-play"                   % "1.5.17"
    val MongoScalaDriver = "org.mongodb.scala"  %% "mongo-scala-driver"                % "4.5.0"
    val jayway = "com.jayway.jsonpath"           % "json-path"                         % "2.7.0"
    val BootstrapTest = "uk.gov.hmrc"           %% "bootstrap-test-play-28"            % "5.4.0"
    val HmrcMongoTest = "uk.gov.hmrc.mongo"     %% "hmrc-mongo-test-play-28"           % "0.59.0"
    val Scalatest = "org.scalatest"             %% "scalatest"                         % "3.1.0"
    val PlayTest = "com.typesafe.play"          %% "play-test"                         % current
    val Flexmark = "com.vladsch.flexmark"        % "flexmark-all"                      % "0.35.10"
    val Scalatestplus = "org.scalatestplus"     %% "scalatestplus-scalacheck"          % "3.1.0.0-RC2"
    val Mockito = "org.mockito"                 %% "mockito-scala"                     % "1.17.5"
    val ServiceIntegrationTest = "uk.gov.hmrc"  %% "service-integration-test"          % "1.1.0-play-28"
    val WireMock = "com.github.tomakehurst"      % "wiremock-standalone"               % "2.27.2"
    val ScalaCheck = "org.scalacheck"           %% "scalacheck"                        % "1.15.4"
    val AkkaTestKit = "com.typesafe.akka"       %% "akka-testkit"                      % "2.6.14"
  }

  import Library._

  val libraries = Seq(
    AkkaHttp,
    ws,
    Bootstrap,
    HmrcMongo,
    HmrcMongoWorkItem,
    Pureconfig,
    Cats,
    Swagger,
    Enumeration,
    MongoScalaDriver,
    jayway,
    BootstrapTest          % Test,
    HmrcMongoTest          % Test,
    Scalatest              % Test,
    PlayTest               % Test,
    Mockito                % Test,
    WireMock               % "it",
    Flexmark               % "test, it",
    Scalatestplus          % "test, it",
    ServiceIntegrationTest % "test, it",
    ScalaCheck             % "it",
    AkkaTestKit            % Test
  )
}
