organization := "com.github.sbt"
version := "0.0.1"

ThisBuild / crossScalaVersions := Seq("2.13.10")
ThisBuild / scalaVersion := crossScalaVersions.value.head

// explicitly don't build `withoutTarget`
ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("withTarget/compile")))

lazy val root = project.in(file(".")).aggregate(withTarget, withoutTarget)
  .settings(
    TaskKey[Unit]("patchIfSbt2") := {
      if (sbtBinaryVersion.value == "2") {
        val yml = file(".github/workflows/ci.yml")
        val targetPath = IO.relativize(baseDirectory.value, target.value).get.replace(java.io.File.separatorChar, '/')
        val withTargetPath = IO.relativize(baseDirectory.value, (withTarget / target).value).get.replace(java.io.File.separatorChar, '/')
        IO.write(
          yml,
          IO.read(yml).replace(
            "run: tar cf targets.tar target withTarget/target project/target",
            s"run: tar cf targets.tar ${targetPath} ${withTargetPath} project/target"
          )
        )
      }
    }
  )

lazy val withTarget = project.in(file("withTarget"))

lazy val withoutTarget = project.in(file("withoutTarget"))
  .settings(githubWorkflowArtifactUpload := false)
