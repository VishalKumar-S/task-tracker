addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.8")
addSbtPlugin("org.foundweekends.giter8" % "sbt-giter8-scaffold" % "0.17.0")

// For building Docker images
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")

// For code formatting
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

// For test coverage reporting
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.11")

// For static code analysis
addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "1.2.6")
