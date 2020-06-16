organization := "com.codecommit"
version := "0.0.1"

ThisBuild / crossScalaVersions := Seq("2.13.2")
ThisBuild / scalaVersion := crossScalaVersions.value.head

// explicitly don't build `withoutTarget`
ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("withTarget/compile")))

def withoutTargetPredicate(step: WorkflowStep): Boolean = step match {
  case step: WorkflowStep.Use => step.params("path").startsWith("withoutTarget")
  case _ => false
}

ThisBuild / githubWorkflowGeneratedUploadSteps :=
  (ThisBuild / githubWorkflowGeneratedUploadSteps).value.filterNot(withoutTargetPredicate)

ThisBuild / githubWorkflowGeneratedDownloadSteps :=
  (ThisBuild / githubWorkflowGeneratedDownloadSteps).value.filterNot(withoutTargetPredicate)

lazy val root = project.in(file(".")).aggregate(withTarget, withoutTarget)

lazy val withTarget = project.in(file("withTarget"))

lazy val withoutTarget = project.in(file("withoutTarget"))
