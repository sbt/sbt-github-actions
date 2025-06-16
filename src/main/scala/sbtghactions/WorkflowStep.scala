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

package sbtghactions

import scala.collection.immutable.ListMap

import scala.concurrent.duration.FiniteDuration

sealed trait WorkflowStep extends Product with Serializable {
  def id: Option[String]
  def name: Option[String]
  def cond: Option[String]
  def env: Map[String, String]
  def timeout: Option[FiniteDuration]
}

object WorkflowStep {

  val DefaultSbtStepPreamble: List[String] = List(s"++ $${{ matrix.scala }}")

  val CheckoutFull: WorkflowStep = Use(
    UseRef.Public(
      "actions",
      "checkout",
      "11bd71901bbe5b1630ceea73d27597364c9af683",
      "v4.2.2"),
    name = Some("Checkout current branch (full)"),
    params = Map("fetch-depth" -> "0"))

  val Checkout: WorkflowStep = Use(
    UseRef.Public(
      "actions",
      "checkout",
      "11bd71901bbe5b1630ceea73d27597364c9af683",
      "v4.2.2"),
    name = Some("Checkout current branch (fast)"))

  def SetupJava(versions: List[JavaSpec]): List[WorkflowStep] =
    versions map {
      case jv @ JavaSpec(JavaSpec.Distribution.GraalVM(Graalvm.Version(graalVersion)), version) =>
        WorkflowStep.Use(
          UseRef.Public(
            "graalvm",
            "setup-graalvm",
            "01ed653ac833fe80569f1ef9f25585ba2811baab",
            "v1.3.3"),
          name = Some(s"Setup GraalVM (${jv.render})"),
          cond = Some(s"matrix.java == '${jv.render}'"),
          params = ListMap(
            "version" -> graalVersion,
            "java-version" -> s"$version",
            "components" -> "native-image",
            "github-token" -> s"$${{ secrets.GITHUB_TOKEN }}",
            "cache" -> "sbt"))
      case jv @ JavaSpec(JavaSpec.Distribution.GraalVM(Graalvm.Distribution(distribution)), version) =>
        WorkflowStep.Use(
          UseRef.Public(
            "graalvm",
            "setup-graalvm",
            "01ed653ac833fe80569f1ef9f25585ba2811baab",
            "v1.3.3"),
          name = Some(s"Setup GraalVM (${jv.render})"),
          cond = Some(s"matrix.java == '${jv.render}'"),
          params = ListMap(
            "java-version" -> s"$version",
            "distribution" -> distribution,
            "components" -> "native-image",
            "github-token" -> s"$${{ secrets.GITHUB_TOKEN }}",
            "cache" -> "sbt"))
      case jv @ JavaSpec(dist, version) =>
        WorkflowStep.Use(
          UseRef.Public(
            "actions",
            "setup-java",
            "c5195efecf7bdfc987ee8bae7a71cb8b11521c00",
            "v4.7.1"),
          name = Some(s"Setup Java (${jv.render})"),
          cond = Some(s"matrix.java == '${jv.render}'"),
          params = ListMap(
            "distribution" -> dist.rendering,
            "java-version" -> version,
            "cache" -> "sbt"))
    }

  def SetupSbt(runnerVersion: Option[String] = None): WorkflowStep =
    Use(
      ref = UseRef.Public(
        "sbt",
        "setup-sbt",
        "6c68d2fe8dfbc0a0534d70101baa2e0420e1a506",
        "v1.1.9"),
      params = runnerVersion match {
        case Some(v) => Map("sbt-runner-version" -> v)
        case None    => Map()
      },
      name = Some("Setup sbt"),
    )

  val Tmate: WorkflowStep = Use(
    UseRef.Public(
      "mxschmitt",
      "action-tmate",
      "ece3d66d6d54a01594acd0ee2e79d1bfb2df136d",
      "v2"), name = Some("Setup tmate session"))

  def ComputeVar(name: String, cmd: String): WorkflowStep =
    Run(
      List("echo \"" + name + "=$(" + cmd + ")\" >> $GITHUB_ENV"),
      name = Some(s"Export $name"))

  def ComputePrependPATH(cmd: String): WorkflowStep =
    Run(
      List("echo \"$(" + cmd + ")\" >> $GITHUB_PATH"),
      name = Some(s"Prepend $$PATH using $cmd"))

  final case class Run(commands: List[String], id: Option[String] = None, name: Option[String] = None, cond: Option[String] = None, env: Map[String, String] = Map(), params: Map[String, String] = Map(), timeout: Option[FiniteDuration] = None) extends WorkflowStep
  final case class Sbt(commands: List[String], id: Option[String] = None, name: Option[String] = None, cond: Option[String] = None, env: Map[String, String] = Map(), params: Map[String, String] = Map(), timeout: Option[FiniteDuration] = None) extends WorkflowStep
  final case class Use(ref: UseRef, params: Map[String, String] = Map(), id: Option[String] = None, name: Option[String] = None, cond: Option[String] = None, env: Map[String, String] = Map(), timeout: Option[FiniteDuration] = None) extends WorkflowStep
}
