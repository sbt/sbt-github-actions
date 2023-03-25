import scala.concurrent.duration._

organization := "com.github.sbt"
version := "0.0.1"

ThisBuild / crossScalaVersions := Seq("2.13.10", "2.12.17")
ThisBuild / scalaVersion := crossScalaVersions.value.head

ThisBuild / githubWorkflowTargetTags += "v*"

ThisBuild / githubWorkflowJavaVersions += JavaSpec.graalvm("22.3.0", "17")
ThisBuild / githubWorkflowPublishTargetBranches += RefPredicate.Equals(Ref.Tag("test"))

ThisBuild / githubWorkflowBuildMatrixAdditions += "test" -> List("this", "is")

ThisBuild / githubWorkflowBuildMatrixInclusions += MatrixInclude(
  Map("test" -> "this"),
  Map("extra" -> "sparta"))

ThisBuild / githubWorkflowBuildMatrixExclusions +=
  MatrixExclude(Map("scala" -> "2.12.17", "test" -> "is"))

ThisBuild / githubWorkflowBuild += WorkflowStep.Run(List("echo yo"))

ThisBuild / githubWorkflowPublish :=
  Seq(
    WorkflowStep.Sbt(
      commands = List("ci-release"),
      name = Some("Publish project"),
    ),
    WorkflowStep.Run(List("echo sup")),
  )
ThisBuild / githubWorkflowBuildTimeout := Some(2.hours)

ThisBuild / githubWorkflowPublishTimeout := Some(1.hour)
