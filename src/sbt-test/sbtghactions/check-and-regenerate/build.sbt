organization := "com.codecommit"
version := "0.0.1"

ThisBuild / crossScalaVersions := Seq("2.13.1", "2.12.10")
ThisBuild / scalaVersion := crossScalaVersions.value.head

ThisBuild / githubWorkflowJavaVersions += "graalvm@20.0.0"
ThisBuild / githubWorkflowPublishTargetBranches += RefPredicate.Equals(Ref.Tag("test"))

ThisBuild / githubWorkflowBuildMatrixAdditions += "test" -> List("this", "is")
