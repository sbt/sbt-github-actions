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
  def name: Option[String]
  def cond: Option[String]
  def env: Map[String, String]
}

object WorkflowStep {

  val CheckoutFull = Use("actions", "checkout", 1, name = Some("Checkout current branch (full)"))
  val Checkout = Use("actions", "checkout", 2, name = Some("Checkout current branch (fast)"))

  val SetupScala = Use("olafurpg", "setup-scala", 5, name = Some("Setup Java and Scala"), params = Map("java-version" -> s"$${{ matrix.java-version }}"))

  val Tmate = Use("mxschmitt", "action-tmate", 2, name = Some("Setup tmate session"))

  def ComputeVar(name: String, cmd: String) =
    Run(List(s"echo ::set-env name=CLONE_DIR::$$($cmd)"), name = Some(s"Export $name"))

  final case class Run(commands: List[String], name: Option[String] = None, cond: Option[String] = None, env: Map[String, String] = Map()) extends WorkflowStep
  final case class Sbt(commands: List[String], name: Option[String] = None, cond: Option[String] = None, env: Map[String, String] = Map()) extends WorkflowStep
  final case class Use(owner: String, repo: String, version: Int, params: Map[String, String] = Map(), name: Option[String] = None, cond: Option[String] = None, env: Map[String, String] = Map()) extends WorkflowStep
}
