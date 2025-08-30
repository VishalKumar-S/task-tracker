lazy val root = (project in file("."))
  .settings(
    name := "notification-service",
    version := "0.1.0",
    scalaVersion := "2.13.12",

    libraryDependencies ++= Seq(
      "com.example" %% "notification-proto" % "0.1.0",           // your local proto jar
      "io.grpc" % "grpc-netty" % "1.62.2",                       // gRPC transport
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % "0.11.13" // ScalaPB gRPC runtime
    )

  )
