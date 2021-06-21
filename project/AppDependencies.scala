
import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % "5.4.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"         % "0.50.0",
    "org.webjars"             % "swagger-ui"                 % "3.50.0"
  )

  val test = Seq(
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % "5.4.0"             % Test,
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % "0.50.0"            % Test,
    "org.scalatest"           %% "scalatest"                  % "3.2.9"             % Test,
    "com.typesafe.play"       %% "play-test"                  % "PlayVersion.current" % Test,
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "5.1.0"             % "test, it"
  )
}
