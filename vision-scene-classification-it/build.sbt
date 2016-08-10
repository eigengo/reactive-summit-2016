import com.typesafe.sbt.packager.docker._

name := "vision-scene-classification-it"

mainClass in Compile := Some("org.eigengo.rsa.scene.v100.MainTest")

mappings in Universal <++= (packageBin in Compile, sourceDirectory) map { (_, src) =>
  packageMapping(
    (src / "main" / "resources") -> "conf"
  ).withContents().mappings.toSeq
}

mappings in Docker += file(s"${name.value}/scripts/docker-entrypoint.sh") -> "/opt/docker-entrypoint.sh"

dockerCommands := dockerCommands.value.flatMap {
  case ep@ExecCmd("ENTRYPOINT", _*) => List(ExecCmd("ENTRYPOINT", "/opt/docker-entrypoint.sh" :: ep.args.toList: _*))
  case other => List(other)
}

enablePlugins(JavaServerAppPackaging, DockerPlugin)
