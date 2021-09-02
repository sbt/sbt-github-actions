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

import sbt._

trait GitHubActionsKeys {

  lazy val githubIsWorkflowBuild = settingKey[Boolean]("Indicates whether or not the current sbt session is running within a GitHub Actions Workflow")

  lazy val githubWorkflowName = settingKey[String]("Contains the name of the currently-running workflow, if defined")

  lazy val githubWorkflowDefinition = settingKey[Map[String, Any]]("The raw (parsed) contents of the workflow manifest file corresponding to this build, recursively converted to Scala")
}

object GitHubActionsKeys extends GitHubActionsKeys
