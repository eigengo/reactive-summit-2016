import sbt.Keys._
import com.trueaccord.scalapb.{ScalaPbPlugin => PB}

scalaVersion in ThisBuild := "2.11.8"

lazy val protocol = project.in(file("protocol"))
  .settings(commonSettings)

lazy val analytics = project.in(file("analytics"))
  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(serverSettings)
  .settings(protobufSettings(protocol))

lazy val analyticsUi = project.in(file("analytics-ui"))
  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(serverSettings)
  .settings(protobufSettings(protocol))
lazy val ingest = project.in(file("ingest"))
  .dependsOn(protocol % PB.protobufConfig.name)
  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(serverSettings)
  .settings(protobufSettings(protocol))

lazy val commonSettings = Seq(
  organization := "org.eigengo",
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")
)

def protobufSettings(protocol: Project) = PB.protobufSettings ++ Seq(
  version in PB.protobufConfig := "2.6.1",
  PB.runProtoc in PB.protobufConfig := (args => com.github.os72.protocjar.Protoc.runProtoc("-v261" +: args.toArray)),
  //sourceDirectory <<= ((sourceDirectory in protocol) in Compile)(_ / "resources"),
  //sourceDirectory := ((sourceDirectory in protocol) in Compile).value,
  //javaSource in PB.protobufConfig <<= (sourceDirectory in Compile)(_ / "generated"),
  PB.externalIncludePath in PB.protobufConfig := ((classDirectory in protocol) in Compile).value,
  sourceDirectories in PB.protobufConfig <+= PB.externalIncludePath in PB.protobufConfig,
  javaSource in PB.protobufConfig <<= (sourceDirectory in Compile)(_ / "generated"),
  libraryDependencies += "com.google.protobuf" % "protobuf-java" % (version in PB.protobufConfig).value % PB.protobufConfig.name
)

lazy val dockerSettings = Seq(
  dockerBaseImage := "cakesolutions/alpine-dcos-base:latest",
  dockerUpdateLatest := true,
  dockerRepository := Some("eigengo"),
  packageName in Docker := s"rs16-${name.value}",
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
