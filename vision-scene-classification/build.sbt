import com.typesafe.sbt.packager.docker._

mainClass in Compile := Some("org.eigengo.rsa.scene.v100.Main")

mappings in Universal <++= (packageBin in Compile, sourceDirectory) map { (_, src) =>
  packageMapping(
    (src / "main" / "resources") -> "conf"
  ).withContents().mappings.toSeq
}

dockerCommands += Cmd("ADD", "http://rsa16-models.s3-website-eu-west-1.amazonaws.com/scene/labels /opt/models/scene/labels")
dockerCommands += Cmd("ADD", "http://rsa16-models.s3-website-eu-west-1.amazonaws.com/scene/config /opt/models/scene/config")
dockerCommands += Cmd("ADD", "http://rsa16-models.s3-website-eu-west-1.amazonaws.com/scene/params /opt/models/scene/params")

enablePlugins(JavaServerAppPackaging, DockerPlugin)
