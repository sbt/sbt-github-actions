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

final case class JavaSpec(dist: JavaSpec.Distribution, version: String) {
  def render: String = dist match {
    case JavaSpec.Distribution.GraalVM(gversion) => s"graal_$gversion@$version"
    case dist => s"${dist.rendering}@$version"
  }
}

object JavaSpec {

  def temurin(version: String): JavaSpec = JavaSpec(Distribution.Temurin, version)
  def graalvm(graal: String, version: String): JavaSpec = JavaSpec(Distribution.GraalVM(graal), version)

  sealed abstract class Distribution(val rendering: String) extends Product with Serializable

  object Distribution {
    case object Temurin extends Distribution("temurin")
    case object Zulu extends Distribution("zulu")
    case object Adopt extends Distribution("adopt-hotspot")
    case object OpenJ9 extends Distribution("adopt-openj9")
    case object Liberica extends Distribution("liberica")
    final case class GraalVM(version: String) extends Distribution(version)
  }
}
