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

lazy val dashboard = project.in(file("dashboard"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(protocol % PB.protobufConfig.name)
  .dependsOn(`protobuf-testkit` % Test)
  .dependsOn(`linter-plugin` % Compile)
  .dependsOn(`scalapb-akka-serializer`)

  .settings(commonSettings)
  .settings(bundleSettings)
  .settings(dockerSettings)
  .settings(serverSettings)
  .settings(linterSettings)
  .settings(protobufSettings(Seq(protocol)))
  .settings(Seq(
      BundleKeys.endpoints := Map(
        "dashboard-service-http" → Endpoint("http", services = Set(URI("http://:8080/"))),
        "dashboard-service-ws" → Endpoint("ws", services = Set(URI("ws://:8080/")))
      )
  ))
  .settings(Seq(
    libraryDependencies += Dependencies.akka.actor,
    libraryDependencies += Dependencies.akka.http.core,
    libraryDependencies += Dependencies.akka.http.experimental,
    libraryDependencies += Dependencies.akka.persistence,
    libraryDependencies += Dependencies.akka.persistenceCassandra,
    libraryDependencies += Dependencies.scalapb.json4s,
    libraryDependencies += Dependencies.troy,
    libraryDependencies += Dependencies.cats,
    libraryDependencies += Dependencies.cakesolutions.akkaKafkaClient,

    libraryDependencies += Dependencies.akka.testKit % Test,
    libraryDependencies += Dependencies.scalaTest % Test,
    libraryDependencies += Dependencies.scalaCheck % Test,
    libraryDependencies += Dependencies.akka.persistenceInMemory % Test
  ))

lazy val `scalapb-akka-serializer` = project.in(file("scalapb-akka-serializer"))
  .settings(commonSettings)
  .settings(protobufSettings(Seq()))
  .settings(Seq(
    libraryDependencies += Dependencies.scalapb.runtime,
    libraryDependencies += Dependencies.akka.actor,
    libraryDependencies += Dependencies.scalaTest % Test,
    libraryDependencies += Dependencies.scalaCheck % Test
  ))

lazy val `deeplearning4j-common` = project.in(file("deeplearning4j-common"))
  .settings(commonSettings)
  .settings(deeplearning4jSettings)
  .settings(Seq(
    libraryDependencies += Dependencies.scalaTest % Test,
    libraryDependencies += Dependencies.scalaCheck % Test
  ))

lazy val `vision-identity` = project.in(file("vision-identity"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(protocol % PB.protobufConfig.name)
  .dependsOn(`protobuf-testkit` % Test)
  .dependsOn(`linter-plugin` % Compile)
  .dependsOn(`deeplearning4j-common`)
  .dependsOn(`scalapb-akka-serializer`)

  .settings(commonSettings)
  .settings(bundleSettings)
  .settings(dockerSettings)
  .settings(serverSettings)
  .settings(linterSettings)
  .settings(localTrainingSettings)
  .settings(protobufSettings(Seq(protocol)))
  .settings(deeplearning4jSettings)
  .settings(Seq(
    libraryDependencies += Dependencies.akka.actor,
    libraryDependencies += Dependencies.akka.persistence,
    libraryDependencies += Dependencies.akka.persistenceCassandra,
    libraryDependencies += Dependencies.scalapb.json4s,
    libraryDependencies += Dependencies.cakesolutions.akkaKafkaClient,
    libraryDependencies += Dependencies.bytedeco.javacv
  ))

lazy val `vision-scene-classification` = project.in(file("vision-scene-classification"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(protocol % PB.protobufConfig.name)
  .dependsOn(`protobuf-testkit` % Test)
  .dependsOn(`linter-plugin` % Compile)
  .dependsOn(`deeplearning4j-common`)

  .settings(commonSettings)
  .settings(bundleSettings)
  .settings(dockerSettings)
  .settings(serverSettings)
  .settings(linterSettings)
  .settings(localTrainingSettings)
  .settings(protobufSettings(Seq(protocol)))
  .settings(deeplearning4jSettings)
  .settings(Seq(
    libraryDependencies += Dependencies.akka.actor,
    libraryDependencies += Dependencies.scalapb.json4s,
    libraryDependencies += Dependencies.cakesolutions.akkaKafkaClient
  ))

lazy val it = project.in(file("it"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(protocol % PB.protobufConfig.name)
  .dependsOn(`protobuf-testkit` % Test)

  .settings(bundleSettings)
  .settings(bundleSettings)
  .settings(commonSettings)
  .settings(dockerSettings)
  .settings(protobufSettings(Seq(protocol)))
  .settings(Seq(
    libraryDependencies += Dependencies.cakesolutions.akkaKafkaClient
  ))

/*
lazy val `vision-text` = project.in(file("vision-text"))
  .enablePlugins(LagomJava)

  .dependsOn(protocol % PB.protobufConfig.name)
  .dependsOn(`protobuf-testkit` % Test)
  .dependsOn(`linter-plugin` % Compile)
  .dependsOn(`scalapb-akka-serializer`)

  .settings(commonSettings)
  .settings(bundleSettings)
  .settings(dockerSettings)
  .settings(serverSettings)
  .settings(linterSettings)
  .settings(protobufSettings(Seq(protocol)))
  .settings(
    libraryDependencies += lagomJavadslPersistence,
    libraryDependencies += lagomJavadslPersistenceCassandra,
    libraryDependencies += lagomJavadslKafkaBroker
  )
  .settings(
    lagomKafkaEnabled in ThisBuild := false,
    lagomKafkaAddress in ThisBuild := "localhost:9092",
    lagomCassandraEnabled in ThisBuild := false,
    lagomCassandraPort in ThisBuild := 9042
  )
*/
lazy val ingest = project.in(file("ingest"))
  .enablePlugins(JavaAppPackaging)
  .dependsOn(protocol % PB.protobufConfig.name)
  .dependsOn(`protobuf-testkit` % Test)
  .dependsOn(`linter-plugin` % Compile)
  .dependsOn(`scalapb-akka-serializer`)

  .settings(commonSettings)
  .settings(bundleSettings)
  .settings(dockerSettings)
  .settings(serverSettings)
  .settings(linterSettings)
  .settings(protobufSettings(Seq(protocol)))
  .settings(Seq(
    libraryDependencies += Dependencies.akka.actor,
    libraryDependencies += Dependencies.akka.persistence,
    libraryDependencies += Dependencies.akka.persistenceCassandra,
    libraryDependencies += Dependencies.akka.http.core,
    libraryDependencies += Dependencies.akka.http.experimental,
    libraryDependencies += Dependencies.scalapb.json4s,
    libraryDependencies += Dependencies.koauth,
    libraryDependencies += Dependencies.conductr.akka,
    libraryDependencies += Dependencies.cakesolutions.akkaKafkaClient
  ))

lazy val `fat-it` = project.in(file("fat-it"))
  .enablePlugins(JavaAppPackaging)

  .settings(commonSettings)
  .settings(bundleSettings)
  .settings(deeplearning4jSettings)
  .dependsOn(`vision-identity`, `vision-scene-classification`, dashboard, it, ingest)

lazy val deeplearning4jSettings = Seq(
  classpathTypes += "maven-plugin",
  libraryDependencies += Dependencies.nd4j.api excludeAll(Dependencies.nd4j.exclusionRules:_*),
  libraryDependencies += Dependencies.nd4j.native() excludeAll(Dependencies.nd4j.exclusionRules:_*),
  libraryDependencies += Dependencies.nd4j.native("linux-x86_64") excludeAll(Dependencies.nd4j.exclusionRules:_*),
  libraryDependencies += Dependencies.deeplearning4j.core,
  libraryDependencies += Dependencies.imageio.core,
  libraryDependencies += Dependencies.bytedeco.javacpp
)

lazy val localTrainingSettings = Seq(
  unmanagedSourceDirectories in Compile <+= sourceDirectory(_ / "train" / "scala"),
  unmanagedSourceDirectories in Compile <+= sourceDirectory(_ / "train" / "java")
)

lazy val commonSettings = Seq(
  organization := "org.eigengo",
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked"),
  resolvers += "Maven central" at "http://repo1.maven.org/maven2/",
  resolvers += Resolver.bintrayRepo("cakesolutions", "maven"),
  resolvers += Resolver.bintrayRepo("krasserm", "maven"),
  resolvers += Resolver.bintrayRepo("tabdulradi", "maven"),
  resolvers += Resolver.jcenterRepo,
  credentials += Credentials(Path.userHome / ".lightbend" / "commercial.credentials"),
  resolvers += "com-mvn" at "https://repo.lightbend.com/commercial-releases/",
  resolvers += Resolver.url("com-ivy", url("https://repo.lightbend.com/commercial-releases/"))(Resolver.ivyStylePatterns),
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
  sourceDirectories in PB.protobufConfig <+= PB.externalIncludePath in PB.protobufConfig
  //libraryDependencies -= "com.google.protobuf" % "protobuf-java" % (version in PB.protobufConfig).value
) ++ protocols.map(p ⇒ PB.externalIncludePath in PB.protobufConfig := ((classDirectory in p) in Compile).value)


import ByteConversions._

lazy val bundleSettings = Seq(
  BundleKeys.memory := 1024.MiB,
  BundleKeys.diskSpace := 5.MB,
  BundleKeys.nrOfCpus := 1,

  SandboxKeys.imageVersion in Global := "1.1.10"
)

lazy val dockerSettings = Seq(
  dockerBaseImage := "cakesolutions/alpine-dcos-base:latest",
  dockerUpdateLatest := true,
  dockerRepository := Some("eigengo"),
  packageName in Docker := s"rsa-${name.value}",
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
