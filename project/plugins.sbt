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

// Refer to the symlinked `sbt-github-actions` sources in the meta-meta-build,
// necessary hack to avoid an infinite project loading recursion.
val sbtGithubActionsSources = ProjectRef(file("project"), "sbtGithubActionsSources")

addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.11")
