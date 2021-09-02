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

import sbt._, Keys._
import sbt.io.Using

import org.yaml.snakeyaml.Yaml

import scala.collection.JavaConverters._

object GitHubActionsPlugin extends AutoPlugin {

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  object autoImport extends GitHubActionsKeys

  import autoImport._

  private[this] def recursivelyConvert(a: Any): Any = a match {
    case map: java.util.Map[_, _] =>
      map.asScala.toMap map { case (k, v) => k -> recursivelyConvert(v) }

    case ls: java.util.List[_] =>
      ls.asScala.toList.map(recursivelyConvert)

    case i: java.lang.Integer =>
      i.intValue

    case b: java.lang.Boolean =>
      b.booleanValue

    case f: java.lang.Float =>
      f.floatValue

    case d: java.lang.Double =>
      d.doubleValue

    case l: java.lang.Long =>
      l.longValue

    case s: String => s
    case null => null
  }

  private val workflowParseSettings = {
    sys.env.get("GITHUB_WORKFLOW") match {
      case Some(workflowName) =>
        Seq(
          githubWorkflowName := workflowName,

          githubWorkflowDefinition := {
            val log = sLog.value
            val name = githubWorkflowName.value
            val base = baseDirectory.value

            if (name != null) {
              val workflowsDir = base / ".github" / "workflows"

              if (workflowsDir.exists() && workflowsDir.isDirectory()) {
                log.info(s"looking for workflow definition in $workflowsDir")

                val results = workflowsDir.listFiles().filter(_.getName.endsWith(".yml")).toList.view flatMap { potential =>
                  Using.fileInputStream(potential) { fis =>
                    Option(new Yaml().load[Any](fis)) collect {
                      case map: java.util.Map[_, _] =>
                        map.asScala.toMap map { case (k, v) => k.toString -> recursivelyConvert(v) }
                    }
                  }
                } filter ( _ get "name" match {
                  case Some(nameValue) =>
                    nameValue == name
                  case None =>
                    log.warn("GitHub action yml file does not contain 'name' key")
                    false
                })

                results.headOption getOrElse {
                  log.warn("unable to find or parse workflow YAML definition")
                  log.warn("assuming the empty map for `githubWorkflowDefinition`")

                  Map()
                }
              } else {
                Map()   // silently pretend nothing is wrong, because we're probably running in a meta-plugin or something random
              }
            } else {
              log.warn("sbt does not appear to be running within GitHub Actions ($GITHUB_WORKFLOW is undefined)")
              log.warn("assuming the empty map for `githubWorkflowDefinition`")

              Map()
            }
          })

      case None =>
        Seq()
    }
  }

  override def buildSettings = workflowParseSettings

  override def globalSettings = Seq(
    githubIsWorkflowBuild := sys.env.get("GITHUB_ACTIONS").map("true" ==).getOrElse(false))
}
