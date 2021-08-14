organization := "com.codecommit"
version := "0.0.1"

ThisBuild / crossScalaVersions := Seq("2.13.6", "2.12.14")
ThisBuild / scalaVersion := crossScalaVersions.value.head
ThisBuild / githubWorkflowIncludeClean := false
