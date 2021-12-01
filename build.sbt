/*
 * Copyright 2020-2021 Daniel Spiewak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

name := "sbt-github-actions"

ThisBuild / baseVersion := "0.14"

ThisBuild / organization := "com.codecommit"
ThisBuild / publishGithubUser := "djspiewak"
ThisBuild / publishFullName := "Daniel Spiewak"
ThisBuild / homepage := Some(url("https://github.com/djspiewak/sbt-github-actions"))
ThisBuild / startYear := Some(2020)
ThisBuild / endYear := Some(2021)

ThisBuild / crossScalaVersions := Seq("2.12.15")

ThisBuild / githubWorkflowOSes := Seq("ubuntu-latest", "macos-latest", "windows-latest")
ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test", "scripted")))
ThisBuild / githubWorkflowJavaVersions += JavaSpec.graalvm("20.3.1", "11")

// dummy publication just to test that setup works
ThisBuild / githubWorkflowPublishTargetBranches :=
  Seq(RefPredicate.Equals(Ref.Branch("main")))

ThisBuild / githubWorkflowPublish := Seq()

sbtPlugin := true
sbtVersion := "1.5.5"

publishMavenStyle := true

scalacOptions += "-Xlint:_,-missing-interpolator"

libraryDependencies += "org.specs2" %% "specs2-core" % "4.12.12" % Test

enablePlugins(SbtPlugin)

scriptedLaunchOpts ++= Seq("-Dplugin.version=" + version.value)
scriptedBufferLog := true
