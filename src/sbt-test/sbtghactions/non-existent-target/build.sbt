organization := "com.github.sbt"
version := "0.0.1"

ThisBuild / crossScalaVersions := Seq("2.13.6")
ThisBuild / scalaVersion := crossScalaVersions.value.head

// explicitly don't build `withoutTarget`
ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("withTarget/compile")))

lazy val root = project.in(file(".")).aggregate(withTarget, withoutTarget)

lazy val withTarget = project.in(file("withTarget"))

lazy val withoutTarget = project.in(file("withoutTarget"))
  .settings(githubWorkflowArtifactUpload := false)
