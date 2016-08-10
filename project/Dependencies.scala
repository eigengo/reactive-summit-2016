import sbt._

object Dependencies {

  val scalaTest  = "org.scalatest"  %% "scalatest"  % "2.2.6"
  // ScalaTest 2.2.6 is not compatible with ScalaCheck > 1.12.5
  val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.12.5"

  val protobuf = "com.google.protobuf" % "protobuf-java" % "3.0.0-beta-3"

  val cats = "org.typelevel" %% "cats" % "0.6.1"

  object cakesolutions {
    val akkaKafkaClient =  "net.cakesolutions" %% "scala-kafka-client-akka" % "0.8.0"
  }

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

  object nd4j {
    private val version = "0.4-rc3.10"
    private lazy val osArchClassifier = {
      val rawOsName = System.getProperty("os.name").toLowerCase
      val rawArch = System.getProperty("os.arch").toLowerCase
      if (rawOsName.startsWith("windows")) s"windows-$rawArch"
      else if (rawOsName.startsWith("linux")) s"linux-$rawArch"
      else if (rawOsName.startsWith("mac os x")) s"macosx-$rawArch"
      else ""
    }

    val exclusionRules = Seq(
      ExclusionRule(organization = "com.google.code.findbugs")
    )

    val api     = "org.nd4j" % "nd4j-api" % version

    // Even though ``native`` includes all native backends, SBT must be explicitly told
    // the classifier in order to pull in the native shared object
    val native  = "org.nd4j" % "nd4j-native" % version classifier "" classifier osArchClassifier
  }

  val javacpp = "org.bytedeco" % "javacpp" % "1.2.2"

  object deeplearning4j {
    private val version = "0.4-rc3.10"

    val core = "org.deeplearning4j" % "deeplearning4j-core" % version
  }

  object imageio {
    private val version = "3.1.1"

    val core = "com.twelvemonkeys.imageio" % "imageio-core" % version
  }

  object boofcv {
    val core = "org.boofcv" % "core" % "0.24.1"
  }

}
