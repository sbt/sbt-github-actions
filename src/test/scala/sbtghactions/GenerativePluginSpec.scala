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

import org.specs2.mutable.Specification

import java.net.URL

class GenerativePluginSpec extends Specification {
  import GenerativePlugin._

  val header = """# This file was automatically generated by sbt-github-actions using the
# githubWorkflowGenerate task. You should add and commit this file to
# your git repository. It goes without saying that you shouldn't edit
# this file by hand! Instead, if you wish to make changes, you should
# change your sbt build configuration to revise the workflow description
# to meet your needs, then regenerate this file.
"""

  "workflow compilation" should {
    "produce the appropriate skeleton around a zero-job workflow" in {
      val expected = header + s"""
        |name: test
        |
        |on:
        |  pull_request:
        |    branches: [main]
        |  push:
        |    branches: [main]
        |
        |jobs:
        |${" " * 2}
        |""".stripMargin

      compileWorkflow("test", List("main"), Nil, Paths.None, PREventType.Defaults, None, Map(), Nil, "sbt") mustEqual expected
    }

    "produce the appropriate skeleton around a zero-job workflow with non-empty tags" in {
      val expected = header + s"""
        |name: test
        |
        |on:
        |  pull_request:
        |    branches: [main]
        |  push:
        |    branches: [main]
        |    tags: [howdy]
        |
        |jobs:
        |${" " * 2}
        |""".stripMargin

      compileWorkflow("test", List("main"), List("howdy"), Paths.None, PREventType.Defaults, None, Map(), Nil, "sbt") mustEqual expected
    }

    "respect non-default pr types" in {
      val expected = header + s"""
        |name: test
        |
        |on:
        |  pull_request:
        |    branches: [main]
        |    types: [ready_for_review, review_requested, opened]
        |  push:
        |    branches: [main]
        |
        |jobs:
        |${" " * 2}
        |""".stripMargin

      compileWorkflow("test", List("main"), Nil, Paths.None, List(PREventType.ReadyForReview, PREventType.ReviewRequested, PREventType.Opened), None, Map(), Nil, "sbt") mustEqual expected
    }

    "compile a one-job workflow targeting multiple branch patterns with a environment variables" in {
      val expected = header + s"""
        |name: test2
        |
        |on:
        |  pull_request:
        |    branches: [main, backport/v*]
        |  push:
        |    branches: [main, backport/v*]
        |
        |permissions:
        |  id-token: write
        |
        |env:
        |  GITHUB_TOKEN: $${{ secrets.GITHUB_TOKEN }}
        |
        |jobs:
        |  build:
        |    name: Build and Test
        |    strategy:
        |      matrix:
        |        os: [ubuntu-latest]
        |        scala: [2.13.6]
        |        java: [temurin@11]
        |    runs-on: $${{ matrix.os }}
        |    steps:
        |      - run: echo Hello World
        |""".stripMargin

      compileWorkflow(
        "test2",
        List("main", "backport/v*"),
        Nil,
        Paths.None,
        PREventType.Defaults,
        Some(Permissions.Specify(Map(
          PermissionScope.IdToken -> PermissionValue.Write
        ))),
        Map(
          "GITHUB_TOKEN" -> s"$${{ secrets.GITHUB_TOKEN }}"),
        List(
          WorkflowJob(
            "build",
            "Build and Test",
            List(WorkflowStep.Run(List("echo Hello World"))))),
        "sbt") mustEqual expected
    }

    "compile a workflow with two jobs" in {
      val expected = header + s"""
        |name: test3
        |
        |on:
        |  pull_request:
        |    branches: [main]
        |  push:
        |    branches: [main]
        |
        |jobs:
        |  build:
        |    name: Build and Test
        |    strategy:
        |      matrix:
        |        os: [ubuntu-latest]
        |        scala: [2.13.6]
        |        java: [temurin@11]
        |    runs-on: $${{ matrix.os }}
        |    steps:
        |      - run: echo yikes
        |
        |  what:
        |    name: If we just didn't
        |    strategy:
        |      matrix:
        |        os: [ubuntu-latest]
        |        scala: [2.13.6]
        |        java: [temurin@11]
        |    runs-on: $${{ matrix.os }}
        |    steps:
        |      - run: whoami
        |""".stripMargin

      compileWorkflow(
        "test3",
        List("main"),
        Nil,
        Paths.None,
        PREventType.Defaults,
        None,
        Map(),
        List(
          WorkflowJob(
            "build",
            "Build and Test",
            List(WorkflowStep.Run(List("echo yikes")))),

          WorkflowJob(
            "what",
            "If we just didn't",
            List(WorkflowStep.Run(List("whoami"))))),
        "") mustEqual expected
    }

    "render a simple container image" in {
      val expected = header + s"""
        |name: test4
        |
        |on:
        |  pull_request:
        |    branches: [main]
        |  push:
        |    branches: [main]
        |
        |jobs:
        |  build:
        |    name: Build and Test
        |    strategy:
        |      matrix:
        |        os: [ubuntu-latest]
        |        scala: [2.13.6]
        |        java: [temurin@11]
        |    runs-on: $${{ matrix.os }}
        |    container: 'not:real-thing'
        |    steps:
        |      - run: echo yikes
        |""".stripMargin

      compileWorkflow(
        "test4",
        List("main"),
        Nil,
        Paths.None,
        PREventType.Defaults,
        None,
        Map(),
        List(
          WorkflowJob(
            "build",
            "Build and Test",
            List(WorkflowStep.Run(List("echo yikes"))),
            container = Some(
              JobContainer("not:real-thing")))),
        "") mustEqual expected
    }

    "render a container with all the trimmings" in {
      val expected = header + s"""
        |name: test4
        |
        |on:
        |  pull_request:
        |    branches: [main]
        |  push:
        |    branches: [main]
        |
        |jobs:
        |  build:
        |    name: Build and Test
        |    strategy:
        |      matrix:
        |        os: [ubuntu-latest]
        |        scala: [2.13.6]
        |        java: [temurin@11]
        |    runs-on: $${{ matrix.os }}
        |    container:
        |      image: 'also:not-real'
        |      credentials:
        |        username: janedoe
        |        password: myvoice
        |      env:
        |        VERSION: 1.0
        |        PATH: /nope
        |      volumes: ['/source:/dest/ination']
        |      ports: [80, 443]
        |      options: '--cpus 1'
        |    steps:
        |      - run: echo yikes
        |""".stripMargin

      compileWorkflow(
        "test4",
        List("main"),
        Nil,
        Paths.None,
        PREventType.Defaults,
        None,
        Map(),
        List(
          WorkflowJob(
            "build",
            "Build and Test",
            List(WorkflowStep.Run(List("echo yikes"))),
            container = Some(
              JobContainer(
                "also:not-real",
                credentials = Some("janedoe" -> "myvoice"),
                env = Map("VERSION" -> "1.0", "PATH" -> "/nope"),
                volumes = Map("/source" -> "/dest/ination"),
                ports = List(80, 443),
                options = List("--cpus", "1"))))),
        "") mustEqual expected
    }

    "render included paths on pull_request and push" in {
      val expected = header + s"""
        |name: test
        |
        |on:
        |  pull_request:
        |    branches: [main]
        |    paths: ['**.scala', '**.sbt']
        |  push:
        |    branches: [main]
        |    paths: ['**.scala', '**.sbt']
        |
        |jobs:
        |${" " * 2}
        |""".stripMargin

      compileWorkflow("test", List("main"), Nil, Paths.Include(List("**.scala", "**.sbt")), PREventType.Defaults, None, Map(), Nil, "sbt") mustEqual expected
    }

    "render ignored paths on pull_request and push" in {
      val expected = header + s"""
        |name: test
        |
        |on:
        |  pull_request:
        |    branches: [main]
        |    paths-ignore: [docs/**]
        |  push:
        |    branches: [main]
        |    paths-ignore: [docs/**]
        |
        |jobs:
        |${" " * 2}
        |""".stripMargin

      compileWorkflow("test", List("main"), Nil, Paths.Ignore(List("docs/**")), PREventType.Defaults, None, Map(), Nil, "sbt") mustEqual expected
    }
  }

  "step compilation" should {
    import WorkflowStep._

    "compile a simple run without a name" in {
      compileStep(Run(List("echo hi")), "") mustEqual "- run: echo hi"
    }

    "compile a simple run with an id" in {
      compileStep(Run(List("echo hi"), id = Some("bippy")), "") mustEqual "- id: bippy\n  run: echo hi"
    }

    "compile a simple run with a name" in {
      compileStep(
        Run(
          List("echo hi"),
          name = Some("nomenclature")),
        "") mustEqual "- name: nomenclature\n  run: echo hi"
    }

    "compile a simple run with a name declaring the shell" in {
      compileStep(
        Run(
          List("echo hi"),
          name = Some("nomenclature")),
        "",
        true) mustEqual "- name: nomenclature\n  shell: bash\n  run: echo hi"
    }

    "omit shell declaration on Use step" in {
      compileStep(
        Use(
          UseRef.Public(
            "repo",
            "slug",
            "v0")),
        "",
        true) mustEqual "- uses: repo/slug@v0"
    }

    "preserve wonky version in Use" in {
      compileStep(Use(UseRef.Public("hello", "world", "v4.0.0")), "", true) mustEqual "- uses: hello/world@v4.0.0"
    }

    "drop Use version prefix on anything that doesn't start with a number" in {
      compileStep(Use(UseRef.Public("hello", "world", "main")), "", true) mustEqual "- uses: hello/world@main"
    }

    "compile sbt using the command provided" in {
      compileStep(
        Sbt(List("show scalaVersion", "compile", "test")),
        "$SBT") mustEqual s"- run: $$SBT ++$${{ matrix.scala }} 'show scalaVersion' compile test"
    }

    "compile sbt with parameters" in {
      compileStep(
        Sbt(List("compile", "test"), params = Map("abc" -> "def", "cafe" -> "@42")),
        "$SBT") mustEqual s"""- run: $$SBT ++$${{ matrix.scala }} compile test
                         |  with:
                         |    abc: def
                         |    cafe: '@42'""".stripMargin
    }

    "compile use without parameters" in {
      "public" >> {
        compileStep(
          Use(UseRef.Public("olafurpg", "setup-scala", "v13")),
          "") mustEqual "- uses: olafurpg/setup-scala@v13"
      }

      "directory" >> {
        compileStep(
          Use(UseRef.Local("foo/bar")),
          "") mustEqual "- uses: ./foo/bar"
      }

      "directory (quantified)" >> {
        compileStep(
          Use(UseRef.Local("./foo/bar")),
          "") mustEqual "- uses: ./foo/bar"
      }

      "docker" >> {
        "docker hub" >> {
          compileStep(
            Use(UseRef.Docker("subarctic-merecat", "2.3.4")),
            "") mustEqual "- uses: docker://subarctic-merecat:2.3.4"
        }

        "hosted" >> {
          compileStep(
            Use(UseRef.Docker("alpine-donkey", "2.3.4", host = Some("host.nope"))),
            "") mustEqual "- uses: docker://host.nope/alpine-donkey:2.3.4"
        }
      }
    }

    "compile use with two parameters" in {
      compileStep(
        Use(UseRef.Public("olafurpg", "setup-scala", "v13"), params = Map("abc" -> "def", "cafe" -> "@42")),
        "") mustEqual "- uses: olafurpg/setup-scala@v13\n  with:\n    abc: def\n    cafe: '@42'"
    }

    "compile use with two parameters and environment variables" in {
      compileStep(
        Use(
          UseRef.Public(
            "derp",
            "nope",
            "v0"),
          params = Map("teh" -> "schizzle", "think" -> "positive"),
          env = Map("hi" -> "there")),
        "") mustEqual "- env:\n    hi: there\n  uses: derp/nope@v0\n  with:\n    teh: schizzle\n    think: positive"
    }

    "compile a run step with multiple commands" in {
      compileStep(Run(List("whoami", "echo yo")), "") mustEqual "- run: |\n    whoami\n    echo yo"
    }

    "compile a run step with a conditional" in {
      compileStep(
        Run(List("users"), cond = Some("true")),
        "") mustEqual "- if: true\n  run: users"
    }

    "compile a run with parameters" in {
      compileStep(
        Run(List("echo foo"), params = Map("abc" -> "def", "cafe" -> "@42")),
        "") mustEqual """- run: echo foo
                        |  with:
                        |    abc: def
                        |    cafe: '@42'""".stripMargin
    }
  }

  "job compilation" should {
    "compile a simple job with two steps" in {
      val results = compileJob(
        WorkflowJob(
          "bippy",
          "Bippity Bop Around the Clock",
          List(
            WorkflowStep.Run(List("echo hello")),
            WorkflowStep.Checkout)),
        "")

      results mustEqual s"""bippy:
  name: Bippity Bop Around the Clock
  strategy:
    matrix:
      os: [ubuntu-latest]
      scala: [2.13.6]
      java: [temurin@11]
  runs-on: $${{ matrix.os }}
  steps:
    - run: echo hello

    - name: Checkout current branch (fast)
      uses: actions/checkout@v2"""
    }

    "compile a job with one step and three oses" in {
      val results = compileJob(
        WorkflowJob(
          "derp",
          "Derples",
          List(
            WorkflowStep.Run(List("echo hello"))),
          oses = List("ubuntu-latest", "windows-latest", "macos-latest")),
        "")

      results mustEqual s"""derp:
  name: Derples
  strategy:
    matrix:
      os: [ubuntu-latest, windows-latest, macos-latest]
      scala: [2.13.6]
      java: [temurin@11]
  runs-on: $${{ matrix.os }}
  steps:
    - shell: bash
      run: echo hello"""
    }

    "compile a job with java setup, two JVMs and two Scalas" in {
      val javas = List(JavaSpec.temurin("11"), JavaSpec.graalvm("20.0.0", "8"))

      val results = compileJob(
        WorkflowJob(
          "abc",
          "How to get to...",
          WorkflowStep.SetupJava(javas),
          scalas = List("2.12.15", "2.13.6"),
          javas = javas),
        "")

      results mustEqual s"""abc:
  name: How to get to...
  strategy:
    matrix:
      os: [ubuntu-latest]
      scala: [2.12.15, 2.13.6]
      java: [temurin@11, graal_20.0.0@8]
  runs-on: $${{ matrix.os }}
  steps:
    - name: Setup Java (temurin@11)
      if: matrix.java == 'temurin@11'
      uses: actions/setup-java@v2
      with:
        distribution: temurin
        java-version: 11

    - name: Setup GraalVM (graal_20.0.0@8)
      if: matrix.java == 'graal_20.0.0@8'
      uses: DeLaGuardo/setup-graalvm@5.0
      with:
        graalvm: 20.0.0
        java: java8"""
    }

    "compile a job with environment variables, conditional, and needs with an sbt step" in {
      val results = compileJob(
        WorkflowJob(
          "nada",
          "Moooo",
          List(
            WorkflowStep.Sbt(List("+compile"))),
          env = Map("not" -> "now"),
          cond = Some("boy != girl"),
          needs = List("unmet")),
        "csbt")

      results mustEqual s"""nada:
  name: Moooo
  needs: [unmet]
  if: boy != girl
  strategy:
    matrix:
      os: [ubuntu-latest]
      scala: [2.13.6]
      java: [temurin@11]
  runs-on: $${{ matrix.os }}
  env:
    not: now
  steps:
    - run: csbt ++$${{ matrix.scala }} +compile"""
    }

    "compile a job with an environment" in {
      val results = compileJob(
        WorkflowJob(
          "publish",
          "Publish Release",
          List(
            WorkflowStep.Sbt(List("ci-release"))),
          environment = Some(JobEnvironment("release"))),
        "csbt")

      results mustEqual s"""publish:
  name: Publish Release
  strategy:
    matrix:
      os: [ubuntu-latest]
      scala: [2.13.6]
      java: [temurin@11]
  runs-on: $${{ matrix.os }}
  environment: release
  steps:
    - run: csbt ++$${{ matrix.scala }} ci-release"""
    }

    "compile a job with specific permissions" in {
      val results = compileJob(
        WorkflowJob(
          "publish",
          "Publish Release",
          List(
            WorkflowStep.Sbt(List("ci-release"))),
          permissions = Some(
            Permissions.Specify(Map(
              PermissionScope.IdToken -> PermissionValue.Write
            ))
          )),
        "csbt")

      results mustEqual s"""publish:
  name: Publish Release
  strategy:
    matrix:
      os: [ubuntu-latest]
      scala: [2.13.6]
      java: [temurin@11]
  runs-on: $${{ matrix.os }}
  permissions:
    id-token: write
  steps:
    - run: csbt ++$${{ matrix.scala }} ci-release"""
    }

    "compile a job with read-all permissions" in {
      val results = compileJob(
        WorkflowJob(
          "publish",
          "Publish Release",
          List(
            WorkflowStep.Sbt(List("ci-release"))),
          permissions = Some(Permissions.ReadAll)
        ),
        "csbt")

      results mustEqual s"""publish:
  name: Publish Release
  strategy:
    matrix:
      os: [ubuntu-latest]
      scala: [2.13.6]
      java: [temurin@11]
  runs-on: $${{ matrix.os }}
  permissions: read-all
  steps:
    - run: csbt ++$${{ matrix.scala }} ci-release"""
    }

    "compile a job with an environment containing a url" in {
      val results = compileJob(
        WorkflowJob(
          "publish",
          "Publish Release",
          List(
            WorkflowStep.Sbt(List("ci-release"))),
          environment = Some(JobEnvironment("release", Some(new URL("https://github.com"))))),
        "csbt")

      results mustEqual s"""publish:
  name: Publish Release
  strategy:
    matrix:
      os: [ubuntu-latest]
      scala: [2.13.6]
      java: [temurin@11]
  runs-on: $${{ matrix.os }}
  environment:
    name: release
    url: 'https://github.com'
  steps:
    - run: csbt ++$${{ matrix.scala }} ci-release"""
    }

    "compile a job with additional matrix components" in {
      val results = compileJob(
        WorkflowJob(
          "bippy",
          "Bippity Bop Around the Clock",
          List(
            WorkflowStep.Run(List("echo ${{ matrix.test }}")),
            WorkflowStep.Checkout),
          matrixAdds = Map("test" -> List("1", "2")),
          matrixFailFast = Some(true)),
        "")

      results mustEqual s"""bippy:
  name: Bippity Bop Around the Clock
  strategy:
    fail-fast: true
    matrix:
      os: [ubuntu-latest]
      scala: [2.13.6]
      java: [temurin@11]
      test: [1, 2]
  runs-on: $${{ matrix.os }}
  steps:
    - run: echo $${{ matrix.test }}

    - name: Checkout current branch (fast)
      uses: actions/checkout@v2"""
    }

    "compile a job with extra runs-on labels" in {
      compileJob(
        WorkflowJob(
          "job",
          "my-name",
          List(
            WorkflowStep.Run(List("echo hello"))),
          runsOnExtraLabels = List("runner-label", "runner-group"),
        ), "") mustEqual """job:
  name: my-name
  strategy:
    matrix:
      os: [ubuntu-latest]
      scala: [2.13.6]
      java: [temurin@11]
  runs-on: [ "${{ matrix.os }}", runner-label, runner-group ]
  steps:
    - run: echo hello"""
    }

    "produce an error when compiling a job with `include` key in matrix" in {
      compileJob(
        WorkflowJob(
          "bippy",
          "Bippity Bop Around the Clock",
          List(),
          matrixAdds = Map("include" -> List("1", "2"))),
        "") must throwA[RuntimeException]
    }

    "produce an error when compiling a job with `exclude` key in matrix" in {
      compileJob(
        WorkflowJob(
          "bippy",
          "Bippity Bop Around the Clock",
          List(),
          matrixAdds = Map("exclude" -> List("1", "2"))),
        "") must throwA[RuntimeException]
    }

    "compile a job with a simple matching inclusion" in {
      val results = compileJob(
        WorkflowJob(
          "bippy",
          "Bippity Bop Around the Clock",
          List(
            WorkflowStep.Run(List("echo ${{ matrix.scala }}"))),
          matrixIncs = List(
            MatrixInclude(
              Map("scala" -> "2.13.6"),
              Map("foo" -> "bar")))),
        "")

      results mustEqual s"""bippy:
  name: Bippity Bop Around the Clock
  strategy:
    matrix:
      os: [ubuntu-latest]
      scala: [2.13.6]
      java: [temurin@11]
      include:
        - scala: 2.13.6
          foo: bar
  runs-on: $${{ matrix.os }}
  steps:
    - run: echo $${{ matrix.scala }}"""
    }

    "produce an error with a non-matching inclusion key" in {
      compileJob(
        WorkflowJob(
          "bippy",
          "Bippity Bop Around the Clock",
          List(
            WorkflowStep.Run(List("echo ${{ matrix.scala }}"))),
          matrixIncs = List(
            MatrixInclude(
              Map("scalanot" -> "2.13.6"),
              Map("foo" -> "bar")))),
        "") must throwA[RuntimeException]
    }

    "produce an error with a non-matching inclusion value" in {
      compileJob(
        WorkflowJob(
          "bippy",
          "Bippity Bop Around the Clock",
          List(
            WorkflowStep.Run(List("echo ${{ matrix.scala }}"))),
          matrixIncs = List(
            MatrixInclude(
              Map("scala" -> "0.12.1"),
              Map("foo" -> "bar")))),
        "") must throwA[RuntimeException]
    }

    "compile a job with a simple matching exclusion" in {
      val results = compileJob(
        WorkflowJob(
          "bippy",
          "Bippity Bop Around the Clock",
          List(
            WorkflowStep.Run(List("echo ${{ matrix.scala }}"))),
          matrixExcs = List(
            MatrixExclude(
              Map("scala" -> "2.13.6")))),
        "")

      results mustEqual s"""bippy:
  name: Bippity Bop Around the Clock
  strategy:
    matrix:
      os: [ubuntu-latest]
      scala: [2.13.6]
      java: [temurin@11]
      exclude:
        - scala: 2.13.6
  runs-on: $${{ matrix.os }}
  steps:
    - run: echo $${{ matrix.scala }}"""
    }

    "produce an error with a non-matching exclusion key" in {
      compileJob(
        WorkflowJob(
          "bippy",
          "Bippity Bop Around the Clock",
          List(
            WorkflowStep.Run(List("echo ${{ matrix.scala }}"))),
          matrixExcs = List(
            MatrixExclude(
              Map("scalanot" -> "2.13.6")))),
        "") must throwA[RuntimeException]
    }

    "produce an error with a non-matching exclusion value" in {
      compileJob(
        WorkflowJob(
          "bippy",
          "Bippity Bop Around the Clock",
          List(
            WorkflowStep.Run(List("echo ${{ matrix.scala }}"))),
          matrixExcs = List(
            MatrixExclude(
              Map("scala" -> "0.12.1")))),
        "") must throwA[RuntimeException]
    }

    "allow a matching JVM exclusion" in {
      compileJob(
        WorkflowJob(
          "bippy",
          "Bippity Bop Around the Clock",
          List(
            WorkflowStep.Run(List("echo ${{ matrix.scala }}"))),
          matrixExcs = List(
            MatrixExclude(
              Map("java" -> JavaSpec.temurin("11").render)))),
        "") must not(throwA[RuntimeException])
    }

    "compile a job with a long list of scala versions" in {
      val results = compileJob(
        WorkflowJob(
          "bippy",
          "Bippity Bop Around the Clock",
          List(
            WorkflowStep.Run(List("echo hello")),
            WorkflowStep.Checkout),
          scalas = List("this", "is", "a", "lot", "of", "versions", "meant", "to", "overflow", "the", "bounds", "checking")),
        "")

      results mustEqual s"""bippy:
  name: Bippity Bop Around the Clock
  strategy:
    matrix:
      os: [ubuntu-latest]
      scala:
        - this
        - is
        - a
        - lot
        - of
        - versions
        - meant
        - to
        - overflow
        - the
        - bounds
        - checking
      java: [temurin@11]
  runs-on: $${{ matrix.os }}
  steps:
    - run: echo hello

    - name: Checkout current branch (fast)
      uses: actions/checkout@v2"""
    }
  }

  "predicate compilation" >> {
    import RefPredicate._
    import Ref._

    "equals" >> {
      compileBranchPredicate("thingy", Equals(Branch("other"))) mustEqual "thingy == 'refs/heads/other'"
    }

    "contains" >> {
      compileBranchPredicate("thingy", Contains(Tag("other"))) mustEqual "(startsWith(thingy, 'refs/tags/') && contains(thingy, 'other'))"
    }

    "startsWith" >> {
      compileBranchPredicate("thingy", StartsWith(Branch("other"))) mustEqual "startsWith(thingy, 'refs/heads/other')"
    }

    "endsWith" >> {
      compileBranchPredicate("thingy", EndsWith(Branch("other"))) mustEqual "(startsWith(thingy, 'refs/heads/') && endsWith(thingy, 'other'))"
    }
  }

  "pr event type compilation" >> {
    import PREventType._

    "assigned" >> (compilePREventType(Assigned) mustEqual "assigned")
    "unassigned" >> (compilePREventType(Unassigned) mustEqual "unassigned")
    "labeled" >> (compilePREventType(Labeled) mustEqual "labeled")
    "unlabeled" >> (compilePREventType(Unlabeled) mustEqual "unlabeled")
    "opened" >> (compilePREventType(Opened) mustEqual "opened")
    "edited" >> (compilePREventType(Edited) mustEqual "edited")
    "closed" >> (compilePREventType(Closed) mustEqual "closed")
    "reopened" >> (compilePREventType(Reopened) mustEqual "reopened")
    "synchronize" >> (compilePREventType(Synchronize) mustEqual "synchronize")
    "ready_for_review" >> (compilePREventType(ReadyForReview) mustEqual "ready_for_review")
    "locked" >> (compilePREventType(Locked) mustEqual "locked")
    "unlocked" >> (compilePREventType(Unlocked) mustEqual "unlocked")
    "review_requested" >> (compilePREventType(ReviewRequested) mustEqual "review_requested")
    "review_request_removed" >> (compilePREventType(ReviewRequestRemoved) mustEqual "review_request_removed")
  }

  "diff" should {
    "highlight the first different character" in {
      val expected =
        """abc
          |
          |def
          |
          |ghi""".stripMargin
      val actual =
        """abc
          |
          |df
          |
          |ghi""".stripMargin
      val expectedDiff =
        """abc
          |
          |df
          | ^ (different character)
          |
          |ghi""".stripMargin
      val actualDiff = GenerativePlugin.diff(expected, actual)
      expectedDiff mustEqual actualDiff
    }
    "highlight the missing lines" in {
      val expected =
        """abc
          |def
          |ghi""".stripMargin
      val actual =
        """abc
          |def""".stripMargin
      val expectedDiff =
        """abc
          |def
          |   ^ (missing lines)""".stripMargin
      val actualDiff = GenerativePlugin.diff(expected, actual)
      expectedDiff mustEqual actualDiff
    }
    "highlight the additionl lines" in {
      val expected =
        """abc
          |def
          |ghi""".stripMargin
      val actual =
        """abc
          |def
          |ghi
          |jkl""".stripMargin
      val expectedDiff =
        """abc
          |def
          |ghi
          |   ^ (additional lines)
          |jkl""".stripMargin
      val actualDiff = GenerativePlugin.diff(expected, actual)
      expectedDiff mustEqual actualDiff
    }
  }
}
