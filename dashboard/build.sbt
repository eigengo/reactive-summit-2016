mainClass in Compile := Some("org.eigengo.rsa.dashboard.v100.Main")

mappings in Universal <++= (packageBin in Compile, sourceDirectory) map { (_, src) =>
  packageMapping(
    (src / "main" / "resources") -> "conf"
  ).withContents().mappings.toSeq
}

enablePlugins(JavaServerAppPackaging, DockerPlugin)
