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

import sbtghactions.Help._

object Help {

  def wrap(str: String): String =
    if (str.indexOf('\n') >= 0)
      "|\n" + indent(str, 1)
    else if (isSafeString(str))
      str
    else
      s"'${str.replace("'", "''")}'"

  private def isSafeString(str: String): Boolean =
    !(str.indexOf(':') >= 0 || // pretend colon is illegal everywhere for simplicity
      str.indexOf('#') >= 0 || // same for comment
      str.indexOf('!') == 0 ||
      str.indexOf('*') == 0 ||
      str.indexOf('-') == 0 ||
      str.indexOf('?') == 0 ||
      str.indexOf('{') == 0 ||
      str.indexOf('}') == 0 ||
      str.indexOf('[') == 0 ||
      str.indexOf(']') == 0 ||
      str.indexOf(',') == 0 ||
      str.indexOf('|') == 0 ||
      str.indexOf('>') == 0 ||
      str.indexOf('@') == 0 ||
      str.indexOf('`') == 0 ||
      str.indexOf('"') == 0 ||
      str.indexOf('\'') == 0 ||
      str.indexOf('&') == 0)

  def indentOnce(output: String): String = indent(output, 1)

  def indent(output: String, level: Int): String = {
    val space = (0 until level * 2).map(_ => ' ').mkString
    (space + output.replace("\n", s"\n$space")).replaceAll("""\n[ ]+\n""", "\n\n")
  }

  def renderParamWithList(paramName: String, items: Seq[String]): String = {
    val rendered = items.map(wrap)
    if (rendered.map(_.length).sum < 40) // just arbitrarily...
      rendered.mkString(s"$paramName: [", ", ", "]")
    else
      rendered.map("- " + _).map(indentOnce).mkString(s"$paramName:\n","\n","\n")
  }

  object SnakeCase {
    private val re = "[A-Z]+".r

    def apply(property: String): String =
      re.replaceAllIn(property.head.toLower +: property.tail, { m => s"_${m.matched.toLowerCase}" })
  }

}

sealed trait Event {
  def render: String
}

final case class Schedule(cron: String) extends Event {

  override def render: String =
    s"""
       |schedule:
       |  - cron: '$cron'
       |""".stripMargin
}

sealed trait WebhookEvent extends Event

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

final case class Workflow(name: String, ons: Seq[Event], jobs: Seq[WorkflowJob]) {

  def render: String =
    s"""|name: ${wrap(name)}
        |
        |on:\n${ons.map(_.render).map(indentOnce).mkString("\n")}""".stripMargin
}
