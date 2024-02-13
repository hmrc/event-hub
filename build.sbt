import com.iheart.sbtPlaySwagger.SwaggerPlugin.autoImport.swaggerDomainNameSpaces

val appName = "event-hub"

Global / majorVersion := 2
Global / scalaVersion := "2.13.12"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(
    play.sbt.PlayScala,
    SbtAutoBuildPlugin,
    SbtGitVersioning,
    SbtDistributablesPlugin,
    SwaggerPlugin
  )
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    libraryDependencies ++= Dependencies.libraries,
    scalacOptions ++= List("-Wconf:cat=unused-imports&src=.*routes.*:s")
  )
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(ScoverageSettings())
  .settings(inConfig(Test)(org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings))

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

lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(`microservice` % "test->test")
