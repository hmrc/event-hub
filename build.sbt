import com.iheart.sbtPlaySwagger.SwaggerPlugin.autoImport.swaggerDomainNameSpaces
import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "event-hub"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(
    play.sbt.PlayScala,
    SbtAutoBuildPlugin,
    SbtGitVersioning,
    SbtDistributablesPlugin,
    SwaggerPlugin
  )
  .settings(
    majorVersion := 2,
    scalaVersion := "2.13.8",
    libraryDependencies ++= Dependencies.libraries
  )
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(ScoverageSettings())
  .settings(inConfig(IntegrationTest)(org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings))

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
compileScalastyle := (Compile / scalastyle).toTask("").value
Compile / compile := ((Compile / compile) dependsOn compileScalastyle).value

PlayKeys.playDefaultPort := 9050

swaggerDomainNameSpaces := Seq(
  "uk.gov.hmrc.eventhub.models"
)

swaggerTarget := baseDirectory.value / "public"
swaggerFileName := "schema.json"
swaggerPrettyJson := true
swaggerRoutesFile := "prod.routes"
swaggerV3 := true

dependencyUpdatesFailBuild := false
Compile / compile := ((Compile / compile) dependsOn dependencyUpdates).value
dependencyUpdatesFilter -= moduleFilter(name = "akka-testkit")
