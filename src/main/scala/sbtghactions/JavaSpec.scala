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
    case JavaSpec.Distribution.GraalVM(Graalvm.Version(gversion)) =>
      s"graal_$gversion@$version"
    case JavaSpec.Distribution.GraalVM(Graalvm.Distribution(distribution)) =>
      s"graal_$distribution@$version"
    case dist => s"${dist.rendering}@$version"
  }
}

/**
 * @see https://github.com/graalvm/setup-graalvm#migrating-from-graalvm-223-or-earlier-to-the-new-graalvm-for-jdk-17-and-later
 */
sealed trait Graalvm extends Product with Serializable {
  private[sbtghactions] def compile: String
}

object Graalvm {
  /**
   * For versions of Graalvm JDK 17 or earlier
   */
  final case class Version(version: String) extends Graalvm {
    override private[sbtghactions] val compile: String = version
  }

  /**
   * For versions of Graalvm JDK 17 or later. Currently valid distributions are
   * graalvm, graalvm-community or mandrel
   */
  final case class Distribution(distribution: String) extends Graalvm {
    override private[sbtghactions] val compile: String = distribution
  }
}

object JavaSpec {

  def temurin(version: String): JavaSpec = JavaSpec(Distribution.Temurin, version)

  def corretto(version: String): JavaSpec = JavaSpec(Distribution.Corretto, version)

  def zulu(version: String): JavaSpec = JavaSpec(Distribution.Zulu, version)

  private[sbtghactions] object JavaVersionExtractor {
    def unapply(version: String): Option[Int] =
      version.split("\\.").headOption.map(_.toInt)
  }

  def graalvm(graal: Graalvm, version: String): JavaSpec = {
    (graal, version) match {
      case (Graalvm.Version(_), JavaVersionExtractor(javaVersion)) if javaVersion > 17 =>
        throw new IllegalArgumentException("Please use Graalvm.Distribution for JDK's newer than 17")
      case (Graalvm.Distribution(_), JavaVersionExtractor(javaVersion)) if javaVersion < 17 =>
        throw new IllegalArgumentException("Graalvm.Distribution is not compatible with JDK's older than 17")
      case _ =>
    }

    JavaSpec(Distribution.GraalVM(graal), version)
  }

  @deprecated("Use graalvm(graal: Graalvm, version: String) instead", "0.17.0")
  def graalvm(graal: String, version: String): JavaSpec =
    graalvm(Graalvm.Version(graal), version: String)

  sealed abstract class Distribution(val rendering: String) extends Product with Serializable

  object Distribution {
    case object Temurin extends Distribution("temurin")
    case object Zulu extends Distribution("zulu")
    case object Adopt extends Distribution("adopt-hotspot")
    case object OpenJ9 extends Distribution("adopt-openj9")
    case object Liberica extends Distribution("liberica")
    case object Corretto extends Distribution("corretto")
    final case class GraalVM(graalvm: Graalvm) extends Distribution(graalvm.compile)
  }
}
