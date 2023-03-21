organization := "com.github.sbt"
version := "0.0.1"

ThisBuild / crossScalaVersions := Seq("2.13.6", "2.12.15")
ThisBuild / scalaVersion := crossScalaVersions.value.head
ThisBuild / githubWorkflowIncludeClean := false
