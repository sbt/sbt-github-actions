organization := "com.github.sbt"
version := "0.0.1"

ThisBuild / crossScalaVersions := Seq("2.13.10", "2.12.17")
ThisBuild / scalaVersion := crossScalaVersions.value.head
ThisBuild / githubWorkflowIncludeClean := false

TaskKey[Unit]("patchIfSbt2") := {
  if (sbtBinaryVersion.value == "2") {
    val yml = file(".github/workflows/ci.yml")
    // sbt 2 nests targets under `target/out/<platform>/scala-<version>/<module>`; the plugin
    // aggregates the Scala-version-independent parent of that, i.e. `target/out/<platform>`
    val targetPath = IO.relativize(baseDirectory.value, target.value.getParentFile.getParentFile).get.replace(java.io.File.separatorChar, '/')
    IO.write(
      yml,
      IO.read(yml).replace(
        "run: tar cf targets.tar target project/target",
        s"run: tar cf targets.tar ${targetPath} project/target"
      )
    )
  }
}
