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

import sbtghactions.RenderFunctions._

sealed trait TriggerEvent {
  def render: String
}

//TODO use CronExpr ADT?
final case class Schedule(cron: String) extends TriggerEvent {

  override def render: String =
    s"""
       |schedule:
       |  - cron: '$cron'
       |""".stripMargin
}

sealed trait WebhookEvent extends TriggerEvent

object WebhookEvent {

  final case class Push(branches: Seq[String], tags: Seq[String]) extends WebhookEvent {

    override def render: String =
      "push:\n" +
        indentOnce { renderBranches + renderTags }

    private def renderBranches =
      renderParamWithList("branches", branches)

    def renderTags: String = if (tags.isEmpty) "" else "\n" + renderParamWithList("tags", tags)

  }

  final case class PullRequest(branches: Seq[String], types: Seq[PREventType]) extends WebhookEvent {

    override def render: String =
      "pull_request:\n" +
        indentOnce { renderBranches + renderTypes }

    private def renderBranches =
      renderParamWithList("branches", branches)

    private def renderTypes =
      if (types == PREventType.Defaults) ""
      else "\n" + renderParamWithList("types", types.map(_.toString).map(SnakeCase.apply))

  }

}





