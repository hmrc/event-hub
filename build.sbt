
val appName = "event-hub"

Global / majorVersion := 3
Global / scalaVersion := "3.4.2"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(
    play.sbt.PlayScala,
    SbtDistributablesPlugin
  )
  .disablePlugins(JUnitXmlReportPlugin)
  .settings(
    libraryDependencies ++= Dependencies.libraries,
    dependencyOverrides ++= Dependencies.dependencyOverrides,
    scalacOptions ++= List("-Xmax-inlines", "64"),
  )
  .settings(ScoverageSettings())
  .settings(inConfig(Test)(org.scalafmt.sbt.ScalafmtPlugin.scalafmtConfigSettings))

lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")
compileScalastyle := (Compile / scalastyle).toTask("").value
Compile / compile := ((Compile / compile) dependsOn compileScalastyle).value

PlayKeys.playDefaultPort := 9050

lazy val it = (project in file("it"))
  .enablePlugins(PlayScala)
  .dependsOn(`microservice` % "test->test")
