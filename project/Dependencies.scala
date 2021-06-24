
import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import play.sbt.PlayImport.ws
import sbt._
import sbt._
import play.core.PlayVersion.current

object Dependencies {

  object Library {
    val Bootstrap = "uk.gov.hmrc"                %% "bootstrap-backend-play-28"         % "5.4.0"
    val HmrcMongo = "uk.gov.hmrc.mongo"          %% "hmrc-mongo-play-28"                % "0.52.0"
    val HmrcMongoWorkItem = "uk.gov.hmrc.mongo"  %% "hmrc-mongo-work-item-repo-play-28" % "0.52.0"
    val Pureconfig = "com.github.pureconfig"     %% "pureconfig"                        % "0.16.0"
    val Cats = "org.typelevel"                   %% "cats-core"                         % "2.6.1"
    val Swagger = "org.webjars"                  % "swagger-ui"                         % "3.50.0"
    val Enumeration =  "com.beachape"            %% "enumeratum-play"                   % "1.5.17"
    val MongoScalaDriver = "org.mongodb.scala"   %% "mongo-scala-driver"                % "4.2.3"
    val BootstrapTest = "uk.gov.hmrc"            %% "bootstrap-test-play-28"            % "5.4.0"
    val HmrcMongoTest = "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28"           % "0.52.0"
    val Scalatest = "org.scalatest"              %% "scalatest"                         % "3.1.0"
    val PlayTest = "com.typesafe.play"           %% "play-test"                         % current
    val Flexmark = "com.vladsch.flexmark"        % "flexmark-all"                       % "0.35.10"
    val Scalatestplus = "org.scalatestplus.play" %% "scalatestplus-play"                % "5.1.0"
    val MockIto = "org.scalatestplus"            %% "mockito-1-10"                      % "3.1.0.0"
    val IntegrationTest = "uk.gov.hmrc"   %% "service-integration-test"          % "1.1.0-play-28"
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
    Enumeration,
    BootstrapTest % Test,
    HmrcMongoTest % Test,
    Scalatest     % Test,
    PlayTest      % Test,
    Flexmark      % "test, it",
    Scalatestplus % "test, it",
    MockIto       % "test, it",
    IntegrationTest  % "test, it"
  )
}