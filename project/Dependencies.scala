import sbt._

object Dependencies {

  val scalaTest  = "org.scalatest"  %% "scalatest"  % "2.2.6"
  // ScalaTest 2.2.6 is not compatible with ScalaCheck > 1.12.5
  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.12.5"

  val protobuf = "com.google.protobuf" % "protobuf-java" % "3.0.0-beta-3"

  object scalapb {
    val runtime = "com.trueaccord.scalapb" %% "scalapb-runtime" % "0.5.30"
    val json4s = "com.trueaccord.scalapb" %% "scalapb-json4s" % "0.1.1"
  }

  object akka {
    val actor = "com.typesafe.akka" %% "akka-actor" % "2.4.7"

    object http {
      val core = "com.typesafe.akka" %% "akka-http-core" % "2.4.7"
      val experimental = "com.typesafe.akka" %% "akka-http-experimental" % "2.4.7"
    }
  }

  object boofcv {
    val core = "org.boofcv" % "core" % "0.24.1"
  }

}
