import sbt._

object Dependencies {

  val scalaTest  = "org.scalatest"  %% "scalatest"  % "2.2.6"
  // ScalaTest 2.2.6 is not compatible with ScalaCheck > 1.12.5
  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.12.5"

  val protobuf = "com.google.protobuf" % "protobuf-java" % "3.0.0-beta-3"
  val scalaPbRuntime = "com.trueaccord.scalapb" %% "scalapb-runtime" % "0.5.30"
  val scalaPbJson4s = "com.trueaccord.scalapb" %% "scalapb-json4s" % "0.1.1"

  val akkaActor = "com.typesafe.akka" %% "akka-actor" % "2.4.7"
  val akkaHttpCore = "com.typesafe.akka" %% "akka-http-core" % "2.4.7"
  val akkaHttpExperimental = "com.typesafe.akka" %% "akka-http-experimental" % "2.4.7"

}
