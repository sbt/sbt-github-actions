organization := "com.codecommit"
version := "0.0.1"

ThisBuild / crossScalaVersions := Seq("2.13.6", "2.12.15")
ThisBuild / scalaVersion := crossScalaVersions.value.head

ThisBuild / githubWorkflowScalaVersions -= "2.12.15"

ThisBuild / githubWorkflowTargetTags += "v*"

ThisBuild / githubWorkflowPublishTargetBranches += RefPredicate.Equals(Ref.Tag("test"))
