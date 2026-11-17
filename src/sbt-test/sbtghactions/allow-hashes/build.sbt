organization := "com.github.sbt"
version := "0.0.1"

// Keep the golden workflow's archive paths stable across sbt layouts.
target := baseDirectory.value / "target"

ThisBuild / scalaVersion := "2.13.10"
