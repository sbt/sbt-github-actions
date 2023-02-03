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

import sbt.*
import sbt.Keys.*
import sbtghactions.RenderFunctions.*

import java.nio.file.FileSystems
import scala.io.Source

object GenerativePlugin extends AutoPlugin {

  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  object autoImport extends GenerativeKeys {
    type WorkflowJob = sbtghactions.WorkflowJob
    val WorkflowJob = sbtghactions.WorkflowJob

    type JobContainer = sbtghactions.JobContainer
    val JobContainer = sbtghactions.JobContainer

    type WorkflowStep = sbtghactions.WorkflowStep
    val WorkflowStep = sbtghactions.WorkflowStep

    type RefPredicate = sbtghactions.RefPredicate
    val RefPredicate = sbtghactions.RefPredicate

    type Ref = sbtghactions.Ref
    val Ref = sbtghactions.Ref

    type UseRef = sbtghactions.UseRef
    val UseRef = sbtghactions.UseRef

    type PREventType = sbtghactions.PREventType
    val PREventType = sbtghactions.PREventType

    type MatrixInclude = sbtghactions.MatrixInclude
    val MatrixInclude = sbtghactions.MatrixInclude

    type MatrixExclude = sbtghactions.MatrixExclude
    val MatrixExclude = sbtghactions.MatrixExclude

    type Paths = sbtghactions.Paths
    val Paths = sbtghactions.Paths

    type JavaSpec = sbtghactions.JavaSpec
    val JavaSpec = sbtghactions.JavaSpec
  }

  import autoImport.*

  private def indent(output: String, level: Int): String = {
    val space = (0 until level * 2).map(_ => ' ').mkString
    (space + output.replace("\n", s"\n$space")).replaceAll("""\n[ ]+\n""", "\n\n")
  }

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
    import PREventType.*

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

  def compileEnvironment(environment: JobEnvironment): String =
    environment.url match {
      case Some(url) =>
        val fields = s"""name: ${wrap(environment.name)}
           |url: ${wrap(url.toString)}""".stripMargin
        s"""environment:
           |${indent(fields, 1)}""".stripMargin
      case None =>
        s"environment: ${wrap(environment.name)}"
    }


  def compileStep(
    step: WorkflowStep,
    sbt: String,
    sbtStepPreamble: List[String] = WorkflowStep.DefaultSbtStepPreamble,
    declareShell: Boolean = false): String = {
    import WorkflowStep.*

    val renderedName = step.name.map(wrap).map("name: " + _ + "\n").getOrElse("")
    val renderedId = step.id.map(wrap).map("id: " + _ + "\n").getOrElse("")
    val renderedCond = step.cond.map(wrap).map("if: " + _ + "\n").getOrElse("")
    val renderedShell = if (declareShell) "shell: bash\n" else ""

    val renderedEnvPre = step.renderEnv
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
      case run: Run =>
        renderRunBody(run.commands, run.params, renderedShell)

      case sbtStep: Sbt =>
        import sbtStep.commands

        val sbtClientMode = sbt.matches("""sbt.* --client($| .*)""")
        val safeCommands = if (sbtClientMode)
          s"'${(sbtStepPreamble ::: commands).mkString("; ")}'"
        else (sbtStepPreamble ::: commands).map { c =>
          if (c.indexOf(' ') >= 0)
            s"'$c'"
          else
            c
        }.mkString(" ")

        renderRunBody(
          commands = List(s"$sbt $safeCommands"),
          params = sbtStep.params,
          renderedShell = renderedShell
        )

      case use: Use =>
        val decl = use.ref match {
          case UseRef.Public(owner, repo, ref) =>
            s"uses: $owner/$repo@$ref"

          case UseRef.Local(path) =>
            val cleaned = if (path.startsWith("./"))
              path
            else
              "./" + path

            s"uses: $cleaned"

          case UseRef.Docker(image, tag, Some(host)) =>
            s"uses: docker://$host/$image:$tag"

          case UseRef.Docker(image, tag, None) =>
            s"uses: docker://$image:$tag"
        }

        decl + use.renderParams
    }

    indentOnce(preamble + body).updated(0, '-')
  }

  def renderRunBody(commands: List[String], params: Map[String, String], renderedShell: String) =
      renderedShell + "run: " + wrap(commands.mkString("\n")) + renderMap(params, "with")

  def compileJob(job: WorkflowJobBase, sbt: String): String =
  job match {
    case ReusableWorkflowJob(id, name, uses, cond, needs) =>
      val renderedNeeds = if (needs.isEmpty)
        ""
      else
        s"\nneeds: [${needs.mkString(", ")}]"

      val renderedCond = cond.map(wrap).map("\nif: " + _).getOrElse("")
      val body =
        s"""|name: ${wrap(name)}$renderedNeeds$renderedCond
            |${uses.render}""".stripMargin

      s"$id:\n${indent(body, 1)}"
    case job: WorkflowJob =>
    val renderedNeeds = if (job.needs.isEmpty)
      ""
    else
      s"\nneeds: [${job.needs.mkString(", ")}]"

    val renderedEnvironment = job.environment.map(compileEnvironment).map("\n" + _).getOrElse("")

    val renderedCond = job.cond.map(wrap).map("\nif: " + _).getOrElse("")

    val renderedContainer = job.container match {
      case Some(JobContainer(image, credentials, env, volumes, ports, options)) =>
        if (credentials.isEmpty && env.isEmpty && volumes.isEmpty && ports.isEmpty && options.isEmpty) {
          "\n" + s"container: ${wrap(image)}"
        } else {
          val renderedImage = s"image: ${wrap(image)}"

          val renderedCredentials = credentials match {
            case Some((username, password)) =>
              s"\ncredentials:\n${indent(s"username: ${wrap(username)}\npassword: ${wrap(password)}", 1)}"

            case None =>
              ""
          }

          val renderedEnv = renderMap(env, "env")

          val renderedVolumes = if (!volumes.isEmpty)
            s"\nvolumes:${compileList(volumes.toList map { case (l, r) => s"$l:$r" }, 1)}"
          else
            ""

          val renderedPorts = if (!ports.isEmpty)
            s"\nports:${compileList(ports.map(_.toString), 1)}"
          else
            ""

          val renderedOptions = if (!options.isEmpty)
            s"\noptions: ${wrap(options.mkString(" "))}"
          else
            ""

          s"\ncontainer:\n${indent(renderedImage + renderedCredentials + renderedEnv + renderedVolumes + renderedPorts + renderedOptions, 1)}"
        }

      case None =>
        ""
    }

    val renderedEnv = renderMap(job.env, "env")
    val renderedPerm = job.permissions.map(_.render).mkString

    List("include", "exclude") foreach { key =>
      if (job.matrixAdds.contains(key)) {
        sys.error(s"key `$key` is reserved and cannot be used in an Actions matrix definition")
      }
    }

    val renderedMatricesPre = job.matrixAdds map {
      case (key, values) => s"$key: ${values.map(wrap).mkString("[", ", ", "]")}"
    } mkString "\n"

    // TODO refactor all of this stuff to use whitelist instead
    val whitelist = Map("os" -> job.oses, "scala" -> job.scalas, "java" -> job.javas.map(_.render)) ++ job.matrixAdds

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

    val matrixOs = if(job.oses.isEmpty) Nil else List("\"${{ matrix.os }}\"")

    val runsOn = if (job.runsOnExtraLabels.isEmpty)
      s"$${{ matrix.os }}"
    else
      (matrixOs ++ job.runsOnExtraLabels).mkString(s"""[ """, ", ", " ]" )

    val renderedFailFast = job.matrixFailFast.fold("")("\n  fail-fast: " + _)

    val renderedOses =
      if (job.oses.isEmpty) "" else s"\n    os:${compileList(job.oses, 3)}"
    val renderedScalas =
      if (job.scalas.isEmpty) "" else s"\n    scala:${compileList(job.scalas, 3)}"
    val renderedJavas =
      if (job.javas.isEmpty) "" else s"\n    java:${compileList(job.javas.map(_.render), 3)}"
    val renderedMatrix =
      if(renderedOses.nonEmpty || renderedScalas.nonEmpty || renderedJavas.nonEmpty) {
        s"\n  matrix:$renderedOses$renderedScalas$renderedJavas$renderedMatrices"
      } else {
        ""
      }

    val renderedStrategy =
      if(renderedFailFast.nonEmpty || renderedMatrix.nonEmpty) {
        s"\nstrategy:$renderedFailFast$renderedMatrix"
      } else {
        ""
      }

    val renderedSteps =
      if (job.steps.nonEmpty) {
        s"""steps:
         |${indent(
            job.steps.map(
              compileStep(_, sbt, job.sbtStepPreamble, declareShell)
              ).mkString("\n\n"),
            1)
        }""".stripMargin
      } else {
        ""
      }

    val body = s"""name: ${wrap(job.name)}${renderedNeeds}${renderedCond}${renderedStrategy}
runs-on: ${runsOn}${renderedEnvironment}${renderedContainer}${renderedEnv}${renderedPerm}
$renderedSteps"""

    s"${job.id}:\n${indentOnce(body)}"
  }

  def compileWorkflow(workflow: Workflow, sbt: String): String = {

    s"""# This file was automatically generated by sbt-github-actions using the
# githubWorkflowGenerate task. You should add and commit this file to
# your git repository. It goes without saying that you shouldn't edit
# this file by hand! Instead, if you wish to make changes, you should
# change your sbt build configuration to revise the workflow description
# to meet your needs, then regenerate this file.

${workflow.render}
jobs:
${indentOnce(workflow.jobs.map(compileJob(_, sbt)).mkString("\n\n"))}
"""
}

  val settingDefaults = Seq(
    githubWorkflowSbtCommand := "sbt",
    githubWorkflowIncludeClean := true,
    // This is currently set to false because of https://github.com/sbt/sbt/issues/6468. When a new SBT version is
    // released that fixes this issue then check for that SBT version (or higher) and set to true.
    githubWorkflowUseSbtThinClient := false,

    githubWorkflowBuildMatrixFailFast := None,
    githubWorkflowBuildMatrixAdditions := Map(),
    githubWorkflowBuildMatrixInclusions := Seq(),
    githubWorkflowBuildMatrixExclusions := Seq(),
    githubWorkflowBuildRunsOnExtraLabels := Seq(),

    githubWorkflowBuildPreamble := Seq(),
    githubWorkflowBuildPostamble := Seq(),
    githubWorkflowBuildSbtStepPreamble := WorkflowStep.DefaultSbtStepPreamble,
    githubWorkflowBuild := Seq(WorkflowStep.Sbt(List("test"), name = Some("Build project"))),

    githubWorkflowPublishPreamble := Seq(),
    githubWorkflowPublishPostamble := Seq(),
    githubWorkflowPublishSbtStepPreamble := Seq(),
    githubWorkflowPublish := Seq(WorkflowStep.Sbt(List("+publish"), name = Some("Publish project"))),
    githubWorkflowPublishTargetBranches := Seq(RefPredicate.Equals(Ref.Branch("main"))),
    githubWorkflowPublishCond := None,

    githubWorkflowJavaVersions := Seq(JavaSpec.temurin("11")),
    githubWorkflowScalaVersions := crossScalaVersions.value,
    githubWorkflowOSes := Seq("ubuntu-latest"),
    githubWorkflowDependencyPatterns := Seq("**/*.sbt", "project/build.properties"),
    githubWorkflowTargetBranches := Seq("**"),
    githubWorkflowTargetTags := Seq(),
    githubWorkflowTargetPaths := Paths.None,

    githubWorkflowEnv := Map("GITHUB_TOKEN" -> s"$${{ secrets.GITHUB_TOKEN }}"),
    githubWorkflowPermissions := None,
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

  override def globalSettings = Seq(
    internalTargetAggregation := Seq(),
    githubWorkflowArtifactUpload := true)

  override def buildSettings = settingDefaults ++ Seq(
    githubWorkflowPREventTypes := PREventType.Defaults,

    githubWorkflowGeneratedUploadSteps := {
      if (githubWorkflowArtifactUpload.value) {
        val sanitized = pathStrs.value map { str =>
          if (str.indexOf(' ') >= 0)    // TODO be less naive
            s"'$str'"
          else
            str
        }

        val tar = WorkflowStep.Run(
          List(s"tar cf targets.tar ${sanitized.mkString(" ")} project/target"),
          name = Some("Compress target directories"))

        val upload = WorkflowStep.Use(
          UseRef.Public(
            "actions",
            "upload-artifact",
            "v3"),
          name = Some(s"Upload target directories"),
          params = Map(
            "name" -> s"target-$${{ matrix.os }}-$${{ matrix.scala }}-$${{ matrix.java }}",
            "path" -> "targets.tar"))

        Seq(tar, upload)
      } else {
        Seq()
      }
    },

    githubWorkflowGeneratedDownloadSteps := {
      val scalas = githubWorkflowScalaVersions.value

      if (githubWorkflowArtifactUpload.value) {
        scalas flatMap { v =>
          val download = WorkflowStep.Use(
            UseRef.Public(
              "actions",
              "download-artifact",
              "v3"),
            name = Some(s"Download target directories ($v)"),
            params = Map(
              "name" -> s"target-$${{ matrix.os }}-$v-$${{ matrix.java }}"))

          val untar = WorkflowStep.Run(
            List(
              "tar xf targets.tar",
              "rm targets.tar"),
            name = Some(s"Inflate target directories ($v)"))

          Seq(download, untar)
        }
      } else {
        Seq()
      }
    },

    githubWorkflowGeneratedCacheSteps := Nil,

    githubWorkflowJobSetup := {
      val autoCrlfOpt = if (githubWorkflowOSes.value.exists(_.contains("windows"))) {
        List(
          WorkflowStep.Run(
            List("git config --global core.autocrlf false"),
            name = Some("Ignore line ending differences in git"),
            cond = windowsGuard))
      } else {
        Nil
      }

      autoCrlfOpt :::
        List(WorkflowStep.CheckoutFull) :::
        WorkflowStep.SetupJava(githubWorkflowJavaVersions.value.toList) :::
        githubWorkflowGeneratedCacheSteps.value.toList
    },

    githubWorkflowCustomWorkflows := Map.empty,
    githubWorkflowGenerationTargets := GenerationTarget.all,

    githubWorkflowGeneratedCI := {
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
          githubWorkflowJobSetup.value.toList :::
            githubWorkflowGeneratedDownloadSteps.value.toList :::
            githubWorkflowPublishPreamble.value.toList :::
            githubWorkflowPublish.value.toList :::
            githubWorkflowPublishPostamble.value.toList,
          sbtStepPreamble = githubWorkflowPublishSbtStepPreamble.value.toList,
          cond = Some(s"github.event_name != 'pull_request' && $publicationCond"),
          scalas = List(scalaVersion.value),
          javas = List(githubWorkflowJavaVersions.value.head),
          needs = List("build"))).filter(_ => !githubWorkflowPublishTargetBranches.value.isEmpty)

      Seq(
        WorkflowJob(
          "build",
          "Build and Test",
          githubWorkflowJobSetup.value.toList :::
            githubWorkflowBuildPreamble.value.toList :::
            WorkflowStep.Sbt(
              List("githubWorkflowCheck"),
              name = Some("Check that workflows are up to date")) ::
            githubWorkflowBuild.value.toList :::
            githubWorkflowBuildPostamble.value.toList :::
            uploadStepsOpt,
          sbtStepPreamble = githubWorkflowBuildSbtStepPreamble.value.toList,
          oses = githubWorkflowOSes.value.toList,
          scalas = githubWorkflowScalaVersions.value.toList,
          javas = githubWorkflowJavaVersions.value.toList,
          matrixFailFast = githubWorkflowBuildMatrixFailFast.value,
          matrixAdds = githubWorkflowBuildMatrixAdditions.value,
          matrixIncs = githubWorkflowBuildMatrixInclusions.value.toList,
          matrixExcs = githubWorkflowBuildMatrixExclusions.value.toList,
          runsOnExtraLabels = githubWorkflowBuildRunsOnExtraLabels.value.toList )) ++ publishJobOpt ++ githubWorkflowAddedJobs.value
    })

  private val generateCiContents = Def task {
    val sbt = if (githubWorkflowUseSbtThinClient.value) {
      githubWorkflowSbtCommand.value + " --client"
    } else {
      githubWorkflowSbtCommand.value
    }
    //compileWorkflow(
    //      "Continuous Integration",
    //      githubWorkflowTargetBranches.value.toList,
    //      githubWorkflowTargetTags.value.toList,
    //      githubWorkflowTargetPaths.value,
    //      githubWorkflowPREventTypes.value.toList,
    //      githubWorkflowPermissions.value,
    //      githubWorkflowEnv.value,
    //      githubWorkflowGeneratedCI.value.toList,
    //      sbt)
    compileWorkflow(
      Workflow(
      "Continuous Integration",
        List(
          WebhookEvent.Push(
            githubWorkflowTargetBranches.value.toList,
            githubWorkflowTargetTags.value.toList,
            githubWorkflowTargetPaths.value
            ),
          WebhookEvent.PullRequest(
            githubWorkflowTargetBranches.value.toList,
            Nil,
            githubWorkflowTargetPaths.value,
            githubWorkflowPREventTypes.value.toList
            )
          ),
        githubWorkflowGeneratedCI.value.toList,
        githubWorkflowEnv.value,
        githubWorkflowPermissions.value,
        ),
      sbt)
  }

  private val readCleanContents = Def task {
    val src = Source.fromURL(getClass.getResource("/clean.yml"))
    try {
      src.mkString
    } finally {
      src.close()
    }
  }

  private val customWorkflowContents = Def task {
    val sbt = if (githubWorkflowUseSbtThinClient.value) {
      githubWorkflowSbtCommand.value + " --client"
    } else {
      githubWorkflowSbtCommand.value
    }

    githubWorkflowCustomWorkflows.value.map {
      case (file, workflow) if file.endsWith(".yml") || file.endsWith(".yaml") =>
        file -> compileWorkflow(workflow, sbt)
      case (file, workflow) =>
        (file + ".yml") -> compileWorkflow(workflow, sbt)
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
    Global / internalTargetAggregation ++= {
      if (githubWorkflowArtifactUpload.value)
        Seq(target.value)
      else
        Seq()
    },

    githubWorkflowGenerate / aggregate := false,
    githubWorkflowCheck / aggregate := false,

    githubWorkflowGenerate := {
      val ciContents = generateCiContents.value
      val includeClean = githubWorkflowIncludeClean.value
      val cleanContents = readCleanContents.value

      val targets = githubWorkflowGenerationTargets.value
      val workflowContents = customWorkflowContents.value
      val workflowsDir = workflowsDirTask.value

      val ciYml = ciYmlFile.value
      val cleanYml = cleanYmlFile.value

      if (targets(GenerationTarget.CI)) {
        IO.write(ciYml, ciContents)
      }

      // TODO think about migration
      if (targets(GenerationTarget.Clean) || includeClean) {
        IO.write(cleanYml, cleanContents)
      }

      if (targets(GenerationTarget.Custom)) {
        workflowContents.foreach { case (file, content) => IO.write(workflowsDir / file, content) }
      }
    },

    githubWorkflowCheck := {
      val expectedCiContents = generateCiContents.value
      val includeClean = githubWorkflowIncludeClean.value
      val expectedCleanContents = readCleanContents.value

      val ciYml = ciYmlFile.value
      val cleanYml = cleanYmlFile.value

      val log = state.value.log

      def reportMismatch(file: File, expected: String, actual: String): Unit = {
        log.error(s"Expected:\n$expected")
        log.error(s"Actual:\n${diff(expected, actual)}")
        sys.error(s"${file.getName} does not contain contents that would have been generated by sbt-github-actions; try running githubWorkflowGenerate")
      }

      def compare(file: File, expected: String): Unit = {
        val actual = IO.read(file)
        if (expected != actual) {
          reportMismatch(file, expected, actual)
        }
      }

      compare(ciYml, expectedCiContents)

      if (includeClean)
        compare(cleanYml, expectedCleanContents)
    })

  private[sbtghactions] def diff(expected: String, actual: String): String = {
    val expectedLines = expected.split("\n", -1)
    val actualLines = actual.split("\n", -1)
    val (lines, _) = expectedLines.zipAll(actualLines, "", "").foldLeft((Vector.empty[String], false)) {
      case ((acc, foundDifference), (expectedLine, actualLine)) if expectedLine == actualLine =>
        (acc :+ actualLine, foundDifference)
      case ((acc, false), ("", actualLine)) =>
        val previousLineLength = acc.lastOption.map(_.length).getOrElse(0)
        val padding = " " * previousLineLength
        val highlight = s"$padding^ (additional lines)"
        (acc :+ highlight :+ actualLine, true)
      case ((acc, false), (_, "")) =>
        val previousLineLength = acc.lastOption.map(_.length).getOrElse(0)
        val padding = " " * previousLineLength
        val highlight = s"$padding^ (missing lines)"
        (acc :+ highlight, true)
      case ((acc, false), (expectedLine, actualLine)) =>
        val sameCount = expectedLine.zip(actualLine).takeWhile({ case (a, b) => a == b }).length
        val padding = " " * sameCount
        val highlight = s"$padding^ (different character)"
        (acc :+ actualLine :+ highlight, true)
      case ((acc, true), (_, "")) =>
        (acc, true)
      case ((acc, true), (_, actualLine)) =>
        (acc :+ actualLine, true)
    }
    lines.mkString("\n")
  }
}
