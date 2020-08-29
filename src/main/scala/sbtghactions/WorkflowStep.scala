/*
 * Copyright 2020 Daniel Spiewak
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
    "actions", "checkout", "v2",
    name = Some("Checkout current branch (full)"),
    params = Map("fetch-depth" -> "0"))

  val Checkout: WorkflowStep = Use("actions", "checkout", "v2", name = Some("Checkout current branch (fast)"))

  val SetupScala: WorkflowStep = Use("olafurpg", "setup-scala", "v5", name = Some("Setup Java and Scala"), params = Map("java-version" -> s"$${{ matrix.java }}"))

  val Tmate: WorkflowStep = Use("mxschmitt", "action-tmate", "v2", name = Some("Setup tmate session"))

  def ComputeVar(name: String, cmd: String): WorkflowStep =
    Run(List(s"echo ::set-env name=$name::$$($cmd)"), name = Some(s"Export $name"))

  final case class Run(commands: List[String], id: Option[String] = None, name: Option[String] = None, cond: Option[String] = None, env: Map[String, String] = Map()) extends WorkflowStep
  final case class Sbt(commands: List[String], id: Option[String] = None, name: Option[String] = None, cond: Option[String] = None, env: Map[String, String] = Map()) extends WorkflowStep
  final case class Use(owner: String, repo: String, ref: String, params: Map[String, String] = Map(), id: Option[String] = None, name: Option[String] = None, cond: Option[String] = None, env: Map[String, String] = Map()) extends WorkflowStep
}
