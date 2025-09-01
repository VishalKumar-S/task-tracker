name := "tasks-service"
organization := "com.vishal"
version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .settings(
    scalaVersion := "2.13.16",

    libraryDependencies ++= Seq(
      guice, // for DI
      jdbc,  // JDBC support
      "mysql" % "mysql-connector-java" % "8.0.33",  // MySQL driver
      "com.typesafe.play" %% "play-slick" % "5.2.0",  // Play-Slick integration
      "com.typesafe.play" %% "play-slick-evolutions" % "5.2.0",   // DB evolutions
      "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test,

      // 👇 gRPC & notification-proto
      "com.example" %% "notification-proto" % "0.1.0",
      "io.grpc" % "grpc-netty" % "1.57.2",
      "io.grpc" % "grpc-protobuf" % "1.57.2",
      "io.grpc" % "grpc-stub" % "1.57.2",

      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.scalamock" %% "scalamock" % "6.0.0" % Test,
      "org.mockito" %% "mockito-scala" % "1.17.31" % Test,
      "com.h2database" % "h2" % "2.2.224" % Test // in-memory DB for repo tests

    )
  )

// Adds additional packages into Twirl
// TwirlKeys.templateImports += "com.vishal.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.vishal.binders._"
