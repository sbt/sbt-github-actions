organization := "com.github.sbt"
version := "0.0.1"

ThisBuild / crossScalaVersions := Seq("2.13.10", "2.12.17")
ThisBuild / scalaVersion := crossScalaVersions.value.head

lazy val root = project.in(file(".")).aggregate(sub)

lazy val sub = project.in(file("sub"))
