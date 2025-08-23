ThisBuild / scalaVersion := "2.12.19"

ThisBuild / githubWorkflowBuild ++= List(
  WorkflowStep.Sbt(List("lots")),
  WorkflowStep.Sbt(List("of")),
  WorkflowStep.Sbt(List("sbt")),
  WorkflowStep.Sbt(List("tasks")),
  WorkflowStep.Sbt(List("run")),
  WorkflowStep.Sbt(List("as", "separate", "steps")),
  WorkflowStep.Sbt(List("using")),
  WorkflowStep.Sbt(List("sbtn"))
)

ThisBuild / githubWorkflowUseSbtThinClient := true

TaskKey[Unit]("patchIfSbt2") := {
  if (sbtBinaryVersion.value == "2") {
    val yml = file(".github/workflows/ci.yml")
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
