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

case class WorkflowRef(
  workflowPath: String,
  ref: String,
  inputs: Map[String, String],
  secrets: Map[String, String]
) {
  lazy val render: String =
    s"uses: $workflowPath@$ref$renderInputs$renderSecrets"

  private def renderInputs = RenderFunctions.renderMap(inputs, "with")
  private def renderSecrets = RenderFunctions.renderMap(secrets, "secrets")
}
