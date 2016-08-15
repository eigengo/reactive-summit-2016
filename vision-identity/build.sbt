import com.typesafe.sbt.packager.docker._

mainClass in Compile := Some("org.eigengo.rsa.identity.v100.Main")

mappings in Universal <++= (packageBin in Compile, sourceDirectory) map { (_, src) =>
  packageMapping(
    (src / "main" / "resources") -> "conf"
  ).withContents().mappings.toSeq
}

dockerCommands += Cmd("ADD", "http://rsa16-models.s3-website-eu-west-1.amazonaws.com/scene/labels /opt/models/identity/labels")
dockerCommands += Cmd("ADD", "http://rsa16-models.s3-website-eu-west-1.amazonaws.com/scene/config /opt/models/identity/config")
dockerCommands += Cmd("ADD", "http://rsa16-models.s3-website-eu-west-1.amazonaws.com/scene/params /opt/models/identity/params")

enablePlugins(JavaServerAppPackaging, DockerPlugin)
