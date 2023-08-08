resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"
resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(
  Resolver.ivyStylePatterns
)
resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("uk.gov.hmrc"       % "sbt-auto-build"        % "3.9.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-distributables"    % "2.2.0")
addSbtPlugin("com.typesafe.play" % "sbt-plugin"            % "2.8.20")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"          % "2.4.0")
addSbtPlugin("com.iheart"        % "sbt-play-swagger"      % "1.0.2")
addSbtPlugin("org.scalastyle"    % "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"         % "1.9.3")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"           % "0.5.3")
addSbtPlugin("uk.gov.hmrc"       % "sbt-service-manager"   % "0.8.0")
