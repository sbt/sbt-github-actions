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

import sbt._, Keys._

import scala.io.Source

import java.io.{BufferedWriter, FileWriter}
import java.nio.file.FileSystems

object GenerativePlugin extends AutoPlugin {

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  object autoImport extends GenerativeKeys {
    type WorkflowJob = sbtghactions.WorkflowJob
    val WorkflowJob = sbtghactions.WorkflowJob

    type WorkflowStep = sbtghactions.WorkflowStep
    val WorkflowStep = sbtghactions.WorkflowStep

    type RefPredicate = sbtghactions.RefPredicate
    val RefPredicate = sbtghactions.RefPredicate

    type Ref = sbtghactions.Ref
    val Ref = sbtghactions.Ref

    type PREventType = sbtghactions.PREventType
    val PREventType = sbtghactions.PREventType

    type MatrixInclude = sbtghactions.MatrixInclude
    val MatrixInclude = sbtghactions.MatrixInclude

    type MatrixExclude = sbtghactions.MatrixExclude
    val MatrixExclude = sbtghactions.MatrixExclude
  }

  import autoImport._

  private def indent(output: String, level: Int): String = {
    val space = (0 until level * 2).map(_ => ' ').mkString
    (space + output.replace("\n", s"\n$space")).replaceAll("""\n[ ]+\n""", "\n\n")
  }

  private def isSafeString(str: String): Boolean =
    !(str.indexOf(':') >= 0 ||    // pretend colon is illegal everywhere for simplicity
      str.indexOf('#') >= 0 ||    // same for comment
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

  private def wrap(str: String): String =
    if (str.indexOf('\n') >= 0)
      "|\n" + indent(str, 1)
    else if (isSafeString(str))
      str
    else
      s"'${str.replace("'", "''")}'"

  def compileList(items: List[String], level: Int): String = {
    val rendered = items.map(wrap)
    if (rendered.map(_.length).sum < 40)   // just arbitrarily...
      rendered.mkString(" [", ", ", "]")
    else
      "\n" + indent(rendered.map("- " + _).mkString("\n"), level)
  }

  def compileListOfSimpleDicts(items: List[Map[String, String]]): String =
    items map { dict =>
      val rendered = dict map {
        case (key, value) => s"$key: $value"
      } mkString "\n"

      "-" + indent(rendered, 1).substring(1)
    } mkString "\n"

  def compilePREventType(tpe: PREventType): String = {
    import PREventType._

    tpe match {
      case Assigned => "assigned"
      case Unassigned => "unassigned"
      case Labeled => "labeled"
      case Unlabeled => "unlabeled"
      case Opened => "opened"
      case Edited => "edited"
      case Closed => "closed"
      case Reopened => "reopened"
      case Synchronize => "synchronize"
      case ReadyForReview => "ready_for_review"
      case Locked => "locked"
      case Unlocked => "unlocked"
      case ReviewRequested => "review_requested"
      case ReviewRequestRemoved => "review_request_removed"
    }
  }

  def compileRef(ref: Ref): String = ref match {
    case Ref.Branch(name) => s"refs/heads/$name"
    case Ref.Tag(name) => s"refs/tags/$name"
  }

  def compileBranchPredicate(target: String, pred: RefPredicate): String = pred match {
    case RefPredicate.Equals(ref) =>
      s"$target == '${compileRef(ref)}'"

    case RefPredicate.Contains(Ref.Tag(name)) =>
      s"(startsWith($target, 'refs/tags/') && contains($target, '$name'))"

    case RefPredicate.Contains(Ref.Branch(name)) =>
      s"(startsWith($target, 'refs/heads/') && contains($target, '$name'))"

    case RefPredicate.StartsWith(ref) =>
      s"startsWith($target, '${compileRef(ref)}')"

    case RefPredicate.EndsWith(Ref.Tag(name)) =>
      s"(startsWith($target, 'refs/tags/') && endsWith($target, '$name'))"

    case RefPredicate.EndsWith(Ref.Branch(name)) =>
      s"(startsWith($target, 'refs/heads/') && endsWith($target, '$name'))"
  }

  def compileEnv(env: Map[String, String], prefix: String = "env"): String =
    if (env.isEmpty) {
      ""
    } else {
      val rendered = env map {
        case (key, value) =>
          if (!isSafeString(key) || key.indexOf(' ') >= 0)
            sys.error(s"'$key' is not a valid environment variable name")

          s"""$key: ${wrap(value)}"""
      }
s"""$prefix:
${indent(rendered.mkString("\n"), 1)}"""
    }

  def compileStep(step: WorkflowStep, sbt: String, declareShell: Boolean = false): String = {
    import WorkflowStep._

    val renderedName = step.name.map(wrap).map("name: " + _ + "\n").getOrElse("")
    val renderedId = step.id.map(wrap).map("id: " + _ + "\n").getOrElse("")
    val renderedCond = step.cond.map(wrap).map("if: " + _ + "\n").getOrElse("")
    val renderedShell = if (declareShell) "shell: bash\n" else ""

    val renderedEnvPre = compileEnv(step.env)
    val renderedEnv = if (renderedEnvPre.isEmpty)
      ""
    else
      renderedEnvPre + "\n"

    val preamblePre = renderedName + renderedId + renderedCond + renderedEnv

    val preamble = if (preamblePre.isEmpty)
      ""
    else
      preamblePre

    val body = step match {
      case Run(commands, _, _, _, _) =>
        renderedShell + "run: " + wrap(commands.mkString("\n"))

      case Sbt(commands, _, _, _, _) =>
        val safeCommands = commands map { c =>
          if (c.indexOf(' ') >= 0)
            s"'$c'"
          else
            c
        }

        renderedShell + "run: " + wrap(s"$sbt ++$${{ matrix.scala }} ${safeCommands.mkString(" ")}")

      case Use(owner, repo, ref, params, _, _, _, _) =>
        val renderedParamsPre = compileEnv(params, prefix = "with")
        val renderedParams = if (renderedParamsPre.isEmpty)
          ""
        else
          "\n" + renderedParamsPre

        s"uses: $owner/$repo@$ref" + renderedParams
    }

    indent(preamble + body, 1).updated(0, '-')
  }

  def compileJob(job: WorkflowJob, sbt: String): String = {
    val renderedNeeds = if (job.needs.isEmpty)
      ""
    else
      s"\nneeds: [${job.needs.mkString(", ")}]"

    val renderedCond = job.cond.map(wrap).map("\nif: " + _).getOrElse("")

    val renderedEnvPre = compileEnv(job.env)
    val renderedEnv = if (renderedEnvPre.isEmpty)
      ""
    else
      "\n" + renderedEnvPre

    List("include", "exclude") foreach { key =>
      if (job.matrixAdds.contains(key)) {
        sys.error(s"key `$key` is reserved and cannot be used in an Actions matrix definition")
      }
    }

    val renderedMatricesPre = job.matrixAdds map {
      case (key, values) => s"$key: ${values.map(wrap).mkString("[", ", ", "]")}"
    } mkString "\n"

    // TODO refactor all of this stuff to use whitelist instead
    val whitelist = Map("os" -> job.oses, "scala" -> job.scalas, "java" -> job.javas) ++ job.matrixAdds

    def checkMatching(matching: Map[String, String]): Unit = {
      matching foreach {
        case (key, value) =>
          if (!whitelist.contains(key)) {
            sys.error(s"inclusion key `$key` was not found in matrix")
          }

          if (!whitelist(key).contains(value)) {
            sys.error(s"inclusion key `$key` was present in matrix, but value `$value` was not in ${whitelist(key)}")
          }
      }
    }

    val renderedIncludesPre = if (job.matrixIncs.isEmpty) {
      renderedMatricesPre
    } else {
      job.matrixIncs.foreach(inc => checkMatching(inc.matching))

      val rendered = compileListOfSimpleDicts(job.matrixIncs.map(i => i.matching ++ i.additions))

      val renderedMatrices = if (renderedMatricesPre.isEmpty)
        ""
      else
        renderedMatricesPre + "\n"

      s"${renderedMatrices}include:\n${indent(rendered, 1)}"
    }

    val renderedExcludesPre = if (job.matrixExcs.isEmpty) {
      renderedIncludesPre
    } else {
      job.matrixExcs.foreach(exc => checkMatching(exc.matching))

      val rendered = compileListOfSimpleDicts(job.matrixExcs.map(_.matching))

      val renderedIncludes = if (renderedIncludesPre.isEmpty)
        ""
      else
        renderedIncludesPre + "\n"

      s"${renderedIncludes}exclude:\n${indent(rendered, 1)}"
    }

    val renderedMatrices = if (renderedExcludesPre.isEmpty)
      ""
    else
      "\n" + indent(renderedExcludesPre, 2)

    val declareShell = job.oses.exists(_.contains("windows"))

    val body = s"""name: ${wrap(job.name)}${renderedNeeds}${renderedCond}
strategy:
  matrix:
    os:${compileList(job.oses, 3)}
    scala:${compileList(job.scalas, 3)}
    java:${compileList(job.javas, 3)}${renderedMatrices}
runs-on: $${{ matrix.os }}${renderedEnv}
steps:
${indent(job.steps.map(compileStep(_, sbt, declareShell = declareShell)).mkString("\n\n"), 1)}"""

    s"${job.id}:\n${indent(body, 1)}"
  }

  def compileWorkflow(
      name: String,
      branches: List[String],
      tags: List[String],
      prEventTypes: List[PREventType],
      env: Map[String, String],
      jobs: List[WorkflowJob],
      sbt: String)
      : String = {

    val renderedEnvPre = compileEnv(env)
    val renderedEnv = if (renderedEnvPre.isEmpty)
      ""
    else
      renderedEnvPre + "\n\n"

    val renderedTypesPre = prEventTypes.map(compilePREventType).mkString("[", ", ", "]")
    val renderedTypes = if (prEventTypes.sortBy(_.toString) == PREventType.Defaults)
      ""
    else
      "\n" + indent("types: " + renderedTypesPre, 2)

    val renderedTags = if (tags.isEmpty)
      ""
    else
s"""
    tags: [${tags.map(wrap).mkString(", ")}]"""

    s"""# This file was automatically generated by sbt-github-actions using the
# githubWorkflowGenerate task. You should add and commit this file to
# your git repository. It goes without saying that you shouldn't edit
# this file by hand! Instead, if you wish to make changes, you should
# change your sbt build configuration to revise the workflow description
# to meet your needs, then regenerate this file.

name: ${wrap(name)}

on:
  pull_request:
    branches: [${branches.map(wrap).mkString(", ")}]$renderedTypes
  push:
    branches: [${branches.map(wrap).mkString(", ")}]$renderedTags

${renderedEnv}jobs:
${indent(jobs.map(compileJob(_, sbt)).mkString("\n\n"), 1)}"""
}

  val settingDefaults = Seq(
    githubWorkflowSbtCommand := "sbt",

    githubWorkflowBuildMatrixAdditions := Map(),
    githubWorkflowBuildMatrixInclusions := Seq(),
    githubWorkflowBuildMatrixExclusions := Seq(),

    githubWorkflowBuildPreamble := Seq(),
    githubWorkflowBuildPostamble := Seq(),
    githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test"), name = Some("Build project"))),

    githubWorkflowPublishPreamble := Seq(),
    githubWorkflowPublishPostamble := Seq(),
    githubWorkflowPublish := Seq(WorkflowStep.Sbt(List("+publish"), name = Some("Publish project"))),
    githubWorkflowPublishTargetBranches := Seq(RefPredicate.Equals(Ref.Branch("master"))),
    githubWorkflowPublishCond := None,

    githubWorkflowJavaVersions := Seq("adopt@1.8"),
    githubWorkflowScalaVersions := crossScalaVersions.value,
    githubWorkflowOSes := Seq("ubuntu-latest"),
    githubWorkflowDependencyPatterns := Seq("**/*.sbt", "project/build.properties"),
    githubWorkflowTargetBranches := Seq("*"),
    githubWorkflowTargetTags := Seq(),

    githubWorkflowEnv := Map("GITHUB_TOKEN" -> s"$${{ secrets.GITHUB_TOKEN }}"),
    githubWorkflowAddedJobs := Seq())

  private lazy val internalTargetAggregation = settingKey[Seq[File]]("Aggregates target directories from all subprojects")

  private val windowsGuard = Some("contains(runner.os, 'windows')")

  private val PlatformSep = FileSystems.getDefault.getSeparator
  private def normalizeSeparators(pathStr: String): String = {
    pathStr.replace(PlatformSep, "/")   // *force* unix separators
  }

  private val pathStrs = Def setting {
    val base = (ThisBuild / baseDirectory).value.toPath

    internalTargetAggregation.value map { file =>
      val path = file.toPath

      if (path.isAbsolute)
        normalizeSeparators(base.relativize(path).toString)
      else
        normalizeSeparators(path.toString)
    }
  }

  // cannot contain '\', '/', '"', ':', '<', '>', '|', '*', or '?'
  private def sanitizeTarget(str: String): String =
    List('\\', '/', '"', ':', '<', '>', '|', '*', '?').foldLeft(str)(_.replace(_, '_'))

  override def globalSettings = Seq(internalTargetAggregation := Seq())

  override def buildSettings = settingDefaults ++ Seq(
    githubWorkflowPREventTypes := PREventType.Defaults,

    githubWorkflowGeneratedUploadSteps := {
      val mainSteps = pathStrs.value map { target =>
        WorkflowStep.Use(
          "actions",
          "upload-artifact",
          "v1",
          name = Some(s"Upload target directory '$target' ($${{ matrix.scala }})"),
          params = Map(
            "name" -> s"target-$${{ matrix.os }}-$${{ matrix.scala }}-$${{ matrix.java }}-${sanitizeTarget(target)}",
            "path" -> target))
      }

      mainSteps :+ WorkflowStep.Use(
        "actions",
        "upload-artifact",
        "v1",
        name = Some(s"Upload target directory 'project/target'"),
        params = Map(
          "name" -> s"target-$${{ matrix.os }}-$${{ matrix.java }}-project_target",
          "path" -> "project/target"))
    },

    githubWorkflowGeneratedDownloadSteps := {
      val mainSteps = pathStrs.value flatMap { target =>
        crossScalaVersions.value map { v =>
          WorkflowStep.Use(
            "actions",
            "download-artifact",
            "v1",
            name = Some(s"Download target directory '$target' ($v)"),
            params = Map(
              "name" -> s"target-$${{ matrix.os }}-$v-$${{ matrix.java }}-${sanitizeTarget(target)}",
              "path" -> target))
        }
      }

      mainSteps :+ WorkflowStep.Use(
        "actions",
        "download-artifact",
        "v1",
        name = Some(s"Download target directory 'project/target'"),
        params = Map(
          "name" -> s"target-$${{ matrix.os }}-$${{ matrix.java }}-project_target",
          "path" -> "project/target"))
    },

    githubWorkflowGeneratedCacheSteps := {
      val hashes = githubWorkflowDependencyPatterns.value map { glob =>
        s"$${{ hashFiles('$glob') }}"
      }

      val hashesStr = hashes.mkString("-")

      Seq(
        WorkflowStep.Use(
          "actions",
          "cache",
          "v1",
          name = Some("Cache ivy2"),
          params = Map(
            "path" -> "~/.ivy2/cache",
            "key" -> s"$${{ runner.os }}-sbt-ivy-cache-$hashesStr")),

        WorkflowStep.Use(
          "actions",
          "cache",
          "v1",
          name = Some("Cache coursier (generic)"),
          params = Map(
            "path" -> "~/.coursier/cache/v1",
            "key" -> s"$${{ runner.os }}-generic-sbt-coursier-cache-$hashesStr")),

        WorkflowStep.Use(
          "actions",
          "cache",
          "v1",
          name = Some("Cache coursier (linux)"),
          cond = Some(s"contains(runner.os, 'linux')"),
          params = Map(
            "path" -> "~/.cache/coursier/v1",
            "key" -> s"$${{ runner.os }}-sbt-coursier-cache-$hashesStr")),

        WorkflowStep.Use(
          "actions",
          "cache",
          "v1",
          name = Some("Cache coursier (macOS)"),
          cond = Some(s"contains(runner.os, 'macos')"),
          params = Map(
            "path" -> "~/Library/Caches/Coursier/v1",
            "key" -> s"$${{ runner.os }}-sbt-coursier-cache-$hashesStr")),

        WorkflowStep.Use(
          "actions",
          "cache",
          "v1",
          name = Some("Cache coursier (windows)"),
          cond = Some(s"contains(runner.os, 'windows')"),
          params = Map(
            "path" -> "~/AppData/Local/Coursier/Cache/v1",
            "key" -> s"$${{ runner.os }}-sbt-coursier-cache-$hashesStr")),

        WorkflowStep.Use(
          "actions",
          "cache",
          "v1",
          name = Some("Cache sbt"),
          params = Map(
            "path" -> "~/.sbt",
            "key" -> s"$${{ runner.os }}-sbt-cache-$hashesStr")))
    },

    githubWorkflowGeneratedCI := {
      val windowsHacksOpt = if (githubWorkflowOSes.value.exists(_.contains("windows"))) {
        // credit: https://stackoverflow.com/a/16754068/9815
        val aliasHack = """git config --global alias.rm-symlinks '!'"$(cat <<'ETX'
__git_rm_symlinks() {
  case "$1" in (-h)
    printf 'usage: git rm-symlinks [symlink] [symlink] [...]\n'
    return 0
  esac
  ppid=$$
  case $# in
    (0) git ls-files -s | grep -E '^120000' | cut -f2 ;;
    (*) printf '%s\n' "$@" ;;
  esac | while IFS= read -r symlink; do
    case "$symlink" in
      (*/*) symdir=${symlink%/*} ;;
      (*) symdir=. ;;
    esac
    git checkout -- "$symlink"
    src="${symdir}/$(cat "$symlink")"
    posix_to_dos_sed='s_^/\([A-Za-z]\)_\1:_;s_/_\\\\_g'
    doslnk=$(printf '%s\n' "$symlink" | sed "$posix_to_dos_sed")
    dossrc=$(printf '%s\n' "$src" | sed "$posix_to_dos_sed")
    if [ -f "$src" ]; then
      rm -f "$symlink"
      cmd //C mklink //H "$doslnk" "$dossrc"
    elif [ -d "$src" ]; then
      rm -f "$symlink"
      cmd //C mklink //J "$doslnk" "$dossrc"
    else
      printf 'error: git-rm-symlink: Not a valid source\n' >&2
      printf '%s =/=> %s  (%s =/=> %s)...\n' \
          "$symlink" "$src" "$doslnk" "$dossrc" >&2
      false
    fi || printf 'ESC[%d]: %d\n' "$ppid" "$?"
    git update-index --assume-unchanged "$symlink"
  done | awk '
    BEGIN { status_code = 0 }
    /^ESC\['"$ppid"'\]: / { status_code = $2 ; next }
    { print }
    END { exit status_code }
  '
}
__git_rm_symlinks
ETX
)"
git config --global alias.rm-symlink '!git rm-symlinks'  # for back-compat."""

        List(
          WorkflowStep.Run(
            List(aliasHack),
            name = Some("Setup rm-symlink alias"),
            cond = windowsGuard),

          WorkflowStep.Run(List("git rm-symlink"), cond = windowsGuard))
      } else {
        Nil
      }

      val autoCrlfOpt = if (githubWorkflowOSes.value.exists(_.contains("windows"))) {
        List(
          WorkflowStep.Run(
            List("git config --global core.autocrlf false"),
            name = Some("Ignore line ending differences in git"),
            cond = windowsGuard))
      } else {
        Nil
      }

      val preamble = autoCrlfOpt ::: List(
        WorkflowStep.Checkout,
        WorkflowStep.SetupScala) :::
        windowsHacksOpt :::
        githubWorkflowGeneratedCacheSteps.value.toList

      val publicationCondPre =
        githubWorkflowPublishTargetBranches.value.map(compileBranchPredicate("github.ref", _)).mkString("(", " || ", ")")

      val publicationCond = githubWorkflowPublishCond.value match {
        case Some(cond) => publicationCondPre + " && (" + cond + ")"
        case None => publicationCondPre
      }

      val uploadStepsOpt = if (githubWorkflowPublishTargetBranches.value.isEmpty && githubWorkflowAddedJobs.value.isEmpty)
        Nil
      else
        githubWorkflowGeneratedUploadSteps.value.toList

      val publishJobOpt = Seq(
        WorkflowJob(
          "publish",
          "Publish Artifacts",
          preamble :::
            githubWorkflowGeneratedDownloadSteps.value.toList :::
            githubWorkflowPublishPreamble.value.toList :::
            githubWorkflowPublish.value.toList :::
            githubWorkflowPublishPostamble.value.toList,
          cond = Some(s"github.event_name != 'pull_request' && $publicationCond"),
          scalas = List(scalaVersion.value),
          javas = List(githubWorkflowJavaVersions.value.head),
          needs = List("build"))).filter(_ => !githubWorkflowPublishTargetBranches.value.isEmpty)

      Seq(
        WorkflowJob(
          "build",
          "Build and Test",
          preamble :::
            githubWorkflowBuildPreamble.value.toList :::
            WorkflowStep.Sbt(
              List("githubWorkflowCheck"),
              name = Some("Check that workflows are up to date")) ::
            githubWorkflowBuild.value.toList :::
            githubWorkflowBuildPostamble.value.toList :::
            uploadStepsOpt,
          oses = githubWorkflowOSes.value.toList,
          scalas = crossScalaVersions.value.toList,
          javas = githubWorkflowJavaVersions.value.toList,
          matrixAdds = githubWorkflowBuildMatrixAdditions.value,
          matrixIncs = githubWorkflowBuildMatrixInclusions.value.toList,
          matrixExcs = githubWorkflowBuildMatrixExclusions.value.toList)) ++ publishJobOpt ++ githubWorkflowAddedJobs.value
    })

  private val generateCiContents = Def task {
    compileWorkflow(
      "Continuous Integration",
      githubWorkflowTargetBranches.value.toList,
      githubWorkflowTargetTags.value.toList,
      githubWorkflowPREventTypes.value.toList,
      githubWorkflowEnv.value,
      githubWorkflowGeneratedCI.value.toList,
      githubWorkflowSbtCommand.value)
  }

  private val readCleanContents = Def task {
    val src = Source.fromURL(getClass.getResource("/clean.yml"))
    try {
      src.getLines().mkString("\n")
    } finally {
      src.close()
    }
  }

  private val workflowsDirTask = Def task {
    val githubDir = baseDirectory.value / ".github"
    val workflowsDir = githubDir / "workflows"

    if (!githubDir.exists()) {
      githubDir.mkdir()
    }

    if (!workflowsDir.exists()) {
      workflowsDir.mkdir()
    }

    workflowsDir
  }

  private val ciYmlFile = Def task {
    workflowsDirTask.value / "ci.yml"
  }

  private val cleanYmlFile = Def task {
    workflowsDirTask.value / "clean.yml"
  }

  override def projectSettings = Seq(
    Global / internalTargetAggregation += target.value,

    githubWorkflowGenerate / aggregate := false,
    githubWorkflowCheck / aggregate := false,

    githubWorkflowGenerate := {
      val ciContents = generateCiContents.value
      val cleanContents = readCleanContents.value

      val ciYml = ciYmlFile.value
      val cleanYml = cleanYmlFile.value

      val ciWriter = new BufferedWriter(new FileWriter(ciYml))
      try {
        ciWriter.write(ciContents)
      } finally {
        ciWriter.close()
      }

      val cleanWriter = new BufferedWriter(new FileWriter(cleanYml))
      try {
        cleanWriter.write(cleanContents)
      } finally {
        cleanWriter.close()
      }
    },

    githubWorkflowCheck := {
      val expectedCiContents = generateCiContents.value
      val expectedCleanContents = readCleanContents.value

      val ciYml = ciYmlFile.value
      val cleanYml = cleanYmlFile.value

      val ciSource = Source.fromFile(ciYml)
      val actualCiContents = try {
        ciSource.getLines().mkString("\n")
      } finally {
        ciSource.close()
      }

      if (expectedCiContents != actualCiContents) {
        sys.error("ci.yml does not contain contents that would have been generated by sbt-github-actions; try running githubWorkflowGenerate")
      }

      val cleanSource = Source.fromFile(cleanYml)
      val actualCleanContents = try {
        cleanSource.getLines().mkString("\n")
      } finally {
        cleanSource.close()
      }

      if (expectedCleanContents != actualCleanContents) {
        sys.error("clean.yml does not contain contents that would have been generated by sbt-github-actions; try running githubWorkflowGenerate")
      }
    })
}
