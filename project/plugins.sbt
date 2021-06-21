
resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)
resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("uk.gov.hmrc"       % "sbt-auto-build"     % "3.0.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-git-versioning" % "2.2.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-distributables" % "2.1.0")
addSbtPlugin("com.typesafe.play" % "sbt-plugin"         % "2.8.7")
addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.16")
addSbtPlugin("com.iheart" % "sbt-play-swagger" % "0.10.4-PLAY2.8")
addSbtPlugin("org.scalastyle"    % "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-bobby"             % "3.3.0")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"         % "1.5.1")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"           % "0.5.3")