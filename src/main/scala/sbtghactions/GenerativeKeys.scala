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

import sbt._

trait GenerativeKeys {

  lazy val githubWorkflowGenerate = taskKey[Unit]("Generates (and overwrites if extant) a ci.yml and clean.yml actions description according to configuration")
  lazy val githubWorkflowCheck = taskKey[Unit]("Checks to see if the ci.yml and clean.yml files are equivalent to what would be generated and errors if otherwise")

  lazy val githubWorkflowGeneratedCI = settingKey[Seq[WorkflowJob]]("The sequence of jobs which will make up the generated ci workflow (ci.yml)")
  lazy val githubWorkflowGeneratedUploadSteps = settingKey[Seq[WorkflowStep]]("The sequence of steps used to upload intermediate build artifacts for an adjacent job")
  lazy val githubWorkflowGeneratedDownloadSteps = settingKey[Seq[WorkflowStep]]("The sequence of steps used to download intermediate build artifacts published by an adjacent job")

  lazy val githubWorkflowSbtCommand = settingKey[String]("The command which invokes sbt (default: sbt")

  lazy val githubWorkflowBuildPreamble = settingKey[Seq[WorkflowStep]]("A list of steps to insert after base setup but before compiling and testing (default: [])")
  lazy val githubWorkflowBuild = settingKey[WorkflowStep]("A workflow step which compiles and tests the project (default: Sbt(List(\"test\")))")

  lazy val githubWorkflowPublishPreamble = settingKey[Seq[WorkflowStep]]("A list of steps to insert after base setup but before publishing (default: [])")
  lazy val githubWorkflowPublish = settingKey[WorkflowStep]("A workflow step which publishes the project (default: Sbt(List(\"+publish\")))")
  lazy val githubWorkflowPublishBranchPatterns = settingKey[Seq[String]]("A set of globs which will be applied to determine which branches get a publication stage; if empty, publish will be skipped (default: [master])")
  lazy val githubWorkflowPublishCond = settingKey[Option[String]]("A set of conditionals to apply to the publish job to further restrict its run (default: [])")

  lazy val githubWorkflowJavaVersions = settingKey[Seq[String]]("A list of Java versions (default: [adopt@1.8])")
  lazy val githubWorkflowScalaVersions = settingKey[Seq[String]]("A list of Scala versions on which to build the project (default: crossScalaVersions.value)")
  lazy val githubWorkflowOSes = settingKey[Seq[String]]("A list of OS names (default: [ubuntu-latest])")

  lazy val githubWorkflowDependencyPatterns = settingKey[Seq[String]]("A list of file globes within the project which affect dependency information (default: [**/*.sbt, project/build.properties])")
  lazy val githubWorkflowTargetBranches = settingKey[Seq[String]]("A list of branch patterns on which to trigger push builds (default: [*])")

  lazy val githubWorkflowEnv = settingKey[Map[String, String]](s"A map of static environment variable assignemnts global to the workflow (default: { GITHUB_TOKEN: $${{ secrets.GITHUB_TOKEN }} })")
  lazy val githubWorkflowAddedJobs = settingKey[Seq[WorkflowJob]]("A list of additional jobs to add to the CI workflow (default: [])")
}

object GenerativeKeys extends GenerativeKeys
