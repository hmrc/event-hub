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
    val Bootstrap = "uk.gov.hmrc"                %% "bootstrap-backend-play-28"         % "5.4.0"
    val HmrcMongo = "uk.gov.hmrc.mongo"          %% "hmrc-mongo-play-28"                % "0.51.0"
    val HmrcMongoWorkItem = "uk.gov.hmrc.mongo"  %% "hmrc-mongo-work-item-repo-play-28" % "0.51.0"
    val Pureconfig = "com.github.pureconfig"     %% "pureconfig"                        % "0.16.0"
    val Cats = "org.typelevel"                   %% "cats-core"                         % "2.6.1"
    val Swagger = "org.webjars"                  % "swagger-ui"                         % "3.50.0"
    val BootstrapTest = "uk.gov.hmrc"            %% "bootstrap-test-play-28"            % "5.4.0"
    val HmrcMongoTest = "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28"           % "0.51.0"
    val Scalatest = "org.scalatest"              %% "scalatest"                         % "3.1.0"
    val PlayTest = "com.typesafe.play"           %% "play-test"                         % current
    val Flexmark = "com.vladsch.flexmark"        % "flexmark-all"                       % "0.35.10"
    val Scalatestplus = "org.scalatestplus.play" %% "scalatestplus-play"                % "5.1.0"
  }

  import Library._

  val libraries = Seq(
    ws,
    Bootstrap,
    HmrcMongo,
    HmrcMongoWorkItem,
    Pureconfig,
    Cats,
    Swagger,
    BootstrapTest % Test,
    HmrcMongoTest % Test,
    Scalatest     % Test,
    PlayTest      % Test,
    Flexmark      % "test, it",
    Scalatestplus % "test, it"
  )
}