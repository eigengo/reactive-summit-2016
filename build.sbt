import sbt.Keys._
import com.trueaccord.scalapb.{ScalaPbPlugin ⇒ PB}

scalaVersion in ThisBuild := "2.11.8"

lazy val protocol = project.in(file("protocol"))
  .settings(commonSettings)

lazy val `protobuf-testkit` = project.in(file("protobuf-testkit"))
  .settings(commonSettings)
  .settings(Seq(
    libraryDependencies += Dependencies.scalaTest,
    libraryDependencies += Dependencies.scalaCheck,
    libraryDependencies += Dependencies.protobuf,
    libraryDependencies += Dependencies.scalapb.runtime
  ))

lazy val `linter-plugin` = project.in(file("linter-plugin"))
  .settings(commonSettings)
  .settings(Seq(
    libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value
  ))

lazy val analytics = project.in(file("analytics"))
  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(serverSettings)
  .settings(protobufSettings(Seq(protocol)))

lazy val `analytics-ui` = project.in(file("analytics-ui"))
  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(serverSettings)
  .settings(protobufSettings(Seq(protocol)))

lazy val `deeplearning4j-common` = project.in(file("deeplearning4j-common"))
  .settings(commonSettings)
  .settings(deeplearning4jSettings)
  .settings(Seq(
    libraryDependencies += Dependencies.cats,
    libraryDependencies += Dependencies.scalaTest % Test,
    libraryDependencies += Dependencies.scalaCheck % Test
  ))

lazy val `vision-identity` = project.in(file("vision-identity"))
  .dependsOn(protocol % PB.protobufConfig.name)
  .dependsOn(`protobuf-testkit` % Test)
  .dependsOn(`linter-plugin` % Compile)
  .dependsOn(`deeplearning4j-common`)

  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(serverSettings)
  .settings(linterSettings)
  .settings(protobufSettings(Seq(protocol)))
  .settings(deeplearning4jSettings)
  .settings(Seq(
    libraryDependencies += Dependencies.akka.actor,
    libraryDependencies += Dependencies.scalapb.json4s
  ))

lazy val `vision-scene-classification` = project.in(file("vision-scene-classification"))
  .dependsOn(protocol % PB.protobufConfig.name)
  .dependsOn(`protobuf-testkit` % Test)
  .dependsOn(`linter-plugin` % Compile)
  .dependsOn(`deeplearning4j-common`)

  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(serverSettings)
  .settings(linterSettings)
  .settings(protobufSettings(Seq(protocol)))
  .settings(deeplearning4jSettings)
  .settings(Seq(
    libraryDependencies += Dependencies.akka.actor,
    libraryDependencies += Dependencies.cats,
    libraryDependencies += Dependencies.scalapb.json4s,
    libraryDependencies += Dependencies.cakesolutions.akkaKafkaClient
  ))

lazy val `vision-scene-classification-it` = project.in(file("vision-scene-classification-it"))
  .dependsOn(protocol % PB.protobufConfig.name)
  .dependsOn(`protobuf-testkit` % Test)

  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(protobufSettings(Seq(protocol)))
  .settings(Seq(
    libraryDependencies += Dependencies.cakesolutions.akkaKafkaClient
  ))

lazy val ingest = project.in(file("ingest"))
  .dependsOn(protocol % PB.protobufConfig.name)
  .dependsOn(`protobuf-testkit` % Test)
  .dependsOn(`linter-plugin` % Compile)

  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(serverSettings)
  .settings(linterSettings)
  .settings(protobufSettings(Seq(protocol)))
  .settings(Seq(
    libraryDependencies += Dependencies.akka.actor,
    libraryDependencies += Dependencies.akka.http.core,
    libraryDependencies += Dependencies.akka.http.experimental,
    libraryDependencies += Dependencies.scalapb.json4s
  ))

// addCompilerPlugin("org.eigengo" %% "linterplugin" % "1.0-SNAPSHOT")

lazy val deeplearning4jSettings = Seq(
  classpathTypes += "maven-plugin",
  libraryDependencies += Dependencies.nd4j.api excludeAll(Dependencies.nd4j.exclusionRules:_*),
  libraryDependencies += Dependencies.nd4j.native() excludeAll(Dependencies.nd4j.exclusionRules:_*),
  libraryDependencies += Dependencies.nd4j.native("linux-x86_64") excludeAll(Dependencies.nd4j.exclusionRules:_*),
  libraryDependencies += Dependencies.deeplearning4j.core,
  libraryDependencies += Dependencies.imageio.core,
  libraryDependencies += Dependencies.javacpp
)

lazy val commonSettings = Seq(
  organization := "org.eigengo",
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked"),
  resolvers += "Maven central" at "http://repo1.maven.org/maven2/",
  resolvers += Resolver.bintrayRepo("cakesolutions", "maven"),
  autoCompilerPlugins := true
)

lazy val linterSettings: Seq[Setting[_]] = Seq(
  scalacOptions += "-Xplugin:" + ((classDirectory in `linter-plugin`) in Compile).value
)

def protobufSettings(protocols: Seq[Project]): Seq[Setting[_]] = PB.protobufSettings ++ Seq(
  version in PB.protobufConfig := "3.0.0",
  PB.runProtoc in PB.protobufConfig := (args => com.github.os72.protocjar.Protoc.runProtoc("-v300" +: args.toArray)),
  javaSource in PB.protobufConfig <<= (sourceDirectory in Compile)(_ / "generated"),
  scalaSource in PB.protobufConfig <<= (sourceDirectory in Compile)(_ / "generated"),
  PB.flatPackage in PB.protobufConfig := true,
  sourceDirectories in PB.protobufConfig <+= PB.externalIncludePath in PB.protobufConfig,
  // The Scala SBT plugin adds a dependency on 2.6.1 protobuf, but we're running on 3.0.0
  libraryDependencies -= "com.google.protobuf" % "protobuf-java" % (version in PB.protobufConfig).value
) ++ protocols.map(p ⇒ PB.externalIncludePath in PB.protobufConfig := ((classDirectory in p) in Compile).value)


lazy val dockerSettings = Seq(
  dockerBaseImage := "cakesolutions/alpine-dcos-base:latest",
  dockerUpdateLatest := true,
  dockerRepository := Some("eigengo"),
  packageName in Docker := s"rsa16-${name.value}",
  maintainer in Docker := "Eigengo <state@eigengo.org>",
  version in Docker := sys.props.getOrElse("tag", default = version.value),
  daemonUser in Docker := "root"
)

lazy val serverSettings = Seq(
  parallelExecution in Test := false,
  testGrouping in Test <<= definedTests in Test map singleTests
)

/*
 * This definition and the server settings are based on those in
 * https://github.com/akka/akka-persistence-cassandra/blob/v0.11/build.sbt
 *
 * This is just so that each test suite that makes use of Cassandra is started in its own JVM. This is required
 * because Cassandra can only be started once per JVM. We'll actually end up running *every* test suite in its own
 * JVM, but that's OK.
 */
def singleTests(tests: Seq[TestDefinition]) = tests.map { test =>
  Tests.Group(
    name = test.name,
    tests = Seq(test),
    runPolicy = Tests.SubProcess(ForkOptions(runJVMOptions = Seq("-Xms512M", "-Xmx1G"))))
}
