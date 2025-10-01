// For building Docker images
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.10.0")

// For code formatting
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

// For test coverage reporting
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.3.0")

// For static code analysis
addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "1.2.6")
