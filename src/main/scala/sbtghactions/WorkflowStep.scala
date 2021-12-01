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

sealed trait WorkflowStep extends Product with Serializable {
  def id: Option[String]
  def name: Option[String]
  def cond: Option[String]
  def env: Map[String, String]
}

object WorkflowStep {

  val CheckoutFull: WorkflowStep = Use(
    UseRef.Public("actions", "checkout", "v2"),
    name = Some("Checkout current branch (full)"),
    params = Map("fetch-depth" -> "0"))

  val Checkout: WorkflowStep = Use(UseRef.Public("actions", "checkout", "v2"), name = Some("Checkout current branch (fast)"))

  def SetupJava(versions: List[JavaSpec]): List[WorkflowStep] =
    versions map {
      case jv @ JavaSpec(JavaSpec.Distribution.GraalVM(graalVersion), version) =>
        WorkflowStep.Use(
          UseRef.Public("DeLaGuardo", "setup-graalvm", "5.0"),
          name = Some(s"Setup GraalVM (${jv.render})"),
          cond = Some(s"matrix.java == '${jv.render}'"),
          params = Map(
            "graalvm" -> graalVersion,
            "java" -> s"java$version"))

      case jv @ JavaSpec(dist, version) =>
        WorkflowStep.Use(
          UseRef.Public("actions", "setup-java", "v2"),
          name = Some(s"Setup Java (${jv.render})"),
          cond = Some(s"matrix.java == '${jv.render}'"),
          params = Map(
            "distribution" -> dist.rendering,
            "java-version" -> version))
    }

  val Tmate: WorkflowStep = Use(UseRef.Public("mxschmitt", "action-tmate", "v2"), name = Some("Setup tmate session"))

  def ComputeVar(name: String, cmd: String): WorkflowStep =
    Run(
      List("echo \"" + name + "=$(" + cmd + ")\" >> $GITHUB_ENV"),
      name = Some(s"Export $name"))

  def ComputePrependPATH(cmd: String): WorkflowStep =
    Run(
      List("echo \"$(" + cmd + ")\" >> $GITHUB_PATH"),
      name = Some(s"Prepend $$PATH using $cmd"))

  final case class Run(commands: List[String], id: Option[String] = None, name: Option[String] = None, cond: Option[String] = None, env: Map[String, String] = Map(), params: Map[String, String] = Map()) extends WorkflowStep
  final case class Sbt(commands: List[String], id: Option[String] = None, name: Option[String] = None, cond: Option[String] = None, env: Map[String, String] = Map(), params: Map[String, String] = Map()) extends WorkflowStep
  final case class Use(ref: UseRef, params: Map[String, String] = Map(), id: Option[String] = None, name: Option[String] = None, cond: Option[String] = None, env: Map[String, String] = Map()) extends WorkflowStep
}
