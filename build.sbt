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

lazy val scala212 = "2.12.18"
ThisBuild / organization := "com.github.sbt"
ThisBuild / crossScalaVersions := Seq(scala212)
ThisBuild / scalaVersion := scala212

ThisBuild / githubWorkflowOSes := Seq("windows-latest", "ubuntu-latest", "macos-latest")
ThisBuild / githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test", "scripted")))
ThisBuild / githubWorkflowJavaVersions += JavaSpec.graalvm(Graalvm.Distribution("graalvm"), "17")

ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches :=
  Seq(
    RefPredicate.StartsWith(Ref.Tag("v")),
    RefPredicate.Equals(Ref.Branch("main"))
  )
ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    commands = List("ci-release"),
    name = Some("Publish project"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)
ThisBuild / version := {
  val orig = (ThisBuild / version).value
  if (orig.endsWith("-SNAPSHOT")) orig.split("""\+""").head + "-SNAPSHOT"
  else orig
}

sbtPlugin := true
pluginCrossBuild / sbtVersion := "1.5.5"

publishMavenStyle := true

scalacOptions +=
  "-Xlint:_,-missing-interpolator"

libraryDependencies += "org.specs2" %% "specs2-core" % "4.19.2" % Test

enablePlugins(SbtPlugin)

scriptedLaunchOpts ++= Seq("-Dplugin.version=" + version.value)
scriptedBufferLog := true
// This sbt version is necessary for CI to work on windows with
// scripted tests, see https://github.com/sbt/sbt/pull/7087
scriptedSbt := "1.9.6"

ThisBuild / homepage := Some(url("https://github.com/sbt/sbt-github-actions"))
ThisBuild / startYear := Some(2020)
ThisBuild / dynverSonatypeSnapshots := true
ThisBuild / developers := List(
  Developer(
    id = "armanbilge",
    name = "Arman Bilge",
    email = "@armanbilge",
    url = url("https://github.com/armanbilge")
  ),
  Developer(
    id = "djspiewak",
    name = "Daniel Spiewak",
    email = "@djspiewak",
    url = url("https://github.com/djspiewak")
  ),
  Developer(
    id = "eed3si9n",
    name = "Eugene Yokota",
    email = "@eed3si9n",
    url = url("https://github.com/eed3si9n")
  ),
  Developer(
    id = "mdedetrich",
    name = "Matthew de Detrich",
    email = "mdedetrich@gmail.com",
    url = url("https://github.com/mdedetrich")
  )
)
ThisBuild / description := "An sbt plugin which makes it easier to build with GitHub Actions"
ThisBuild / licenses := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / pomIncludeRepository := { _ =>
  false
}
ThisBuild / publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
  else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true
Global / excludeLintKeys ++= Set(pomIncludeRepository, publishMavenStyle)
