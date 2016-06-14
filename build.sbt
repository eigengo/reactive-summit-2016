import sbt.Keys._

scalaVersion in ThisBuild := "2.12.0-M4"

lazy val protocol = project.in(file("protocol"))
  .settings(commonSettings)

lazy val analytics = project.in(file("analytics"))
  .dependsOn(protocol % "compile->compile")
  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(serverSettings)

lazy val analyticsUi = project.in(file("analytics-ui"))
  .dependsOn(protocol % "compile->compile")
  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(serverSettings)

lazy val ingest = project.in(file("ingest"))
  .dependsOn(protocol % "compile->compile")
  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(serverSettings)

lazy val commonSettings = Seq(
  organization := "org.eigengo",
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked")
)

lazy val dockerSettings = Seq(
  dockerBaseImage := "cakesolutions/alpine-dcos-base:latest",
  dockerUpdateLatest := true,
  dockerRepository := Some("cakesolutions"),
  packageName in Docker := s"iot-demo-${name.value}",
  maintainer in Docker := "Cake Solutions <devops@cakesolutions.net>",
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
