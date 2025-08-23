organization := "com.github.sbt"
version := "0.0.1"

ThisBuild / crossScalaVersions := Seq("2.13.10", "2.12.17")
ThisBuild / scalaVersion := crossScalaVersions.value.head

ThisBuild / githubWorkflowScalaVersions -= "2.12.17"

ThisBuild / githubWorkflowTargetTags += "v*"

ThisBuild / githubWorkflowPublishTargetBranches += RefPredicate.Equals(Ref.Tag("test"))

TaskKey[Unit]("patchIfSbt2") := {
  if (sbtBinaryVersion.value == "2") {
    val yml = file("expected-ci.yml")
    val targetPath = IO.relativize(baseDirectory.value, target.value).get.replace(java.io.File.separatorChar, '/')
    IO.write(
      yml,
      IO.read(yml).replace(
        "run: tar cf targets.tar target project/target",
        s"run: tar cf targets.tar ${targetPath} project/target"
      )
    )
  }
}
