import sbtprotoc.ProtocPlugin.autoImport.PB

name := "notification-proto"
organization := "com.example"
version := "0.1.0"
scalaVersion := "2.13.12"

enablePlugins(ProtocPlugin)F

Compile / PB.targets := Seq(
  scalapb.gen(grpc = true) -> (Compile / sourceManaged).value
)

libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
  "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
  "io.grpc" % "grpc-stub" % scalapb.compiler.Version.grpcJavaVersion,
  "io.grpc" % "grpc-protobuf" % scalapb.compiler.Version.grpcJavaVersion
)
