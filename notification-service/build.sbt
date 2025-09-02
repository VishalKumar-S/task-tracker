import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.docker.DockerPlugin

lazy val root = (project in file("."))
  .settings(
    name := "notification-service",
    version := "0.1.0",
    scalaVersion := "2.13.16",

    libraryDependencies ++= Seq(
      "com.example" %% "notification-proto" % "0.1.0",           // your local proto jar
      "io.grpc" % "grpc-netty" % "1.62.2",                       // gRPC transport
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % "0.11.13", // ScalaPB gRPC runtime
      "mysql" % "mysql-connector-java" % "8.0.33",  // MySQL driver
      "org.flywaydb" % "flyway-core" % "10.17.0",   // Flyway
      "org.flywaydb" % "flyway-mysql" % "10.17.0",  // Flyway MySQL support
      "com.typesafe" % "config" % "1.4.3",                        // For loading application.conf
      "com.typesafe.slick" %% "slick" % "3.5.1",                  // Slick for DB access
      "com.typesafe.slick" %% "slick-hikaricp" % "3.5.1",         // Slick connection pooling
      "ch.qos.logback" % "logback-classic" % "1.5.6",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.scalamock" %% "scalamock" % "6.0.0" % Test,
      "org.mockito" %% "mockito-scala" % "1.17.31" % Test,
      "com.h2database" % "h2" % "2.2.224" % Test // in-memory DB for repo tests




    ),
    // --- Packaging and Docker Settings ---
    // Tell sbt where your main method is
    mainClass in Compile := Some("notifications.NotificationServer"),
    // Use a slim, secure JRE for the final image
    dockerBaseImage := "openjdk:17-jdk-slim"

  ).enablePlugins(JavaAppPackaging, DockerPlugin)