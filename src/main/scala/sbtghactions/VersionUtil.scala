/*
 * Copyright 2021 Daniel Spiewak
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

private[sbtghactions] object VersionUtil {
  def inferPublishVersions(versions: Seq[String]): Seq[String] = {
    val versionMappings = versions.map { version =>
      (inferPublishVersion(version), version)
    }
    versionMappings.map(_._1).distinct.flatMap { mappedVersion =>
      versionMappings.find(_._1 == mappedVersion).map(_._2)
    }
  }

  private def inferPublishVersion(version: String): String = {
    if (version.startsWith("2.")) {
      val splits = version.split("""\.""")
      if (splits.length >= 2) {
        s"2.${splits(1)}"
      } else {
        version
      }
    } else if (version.startsWith("3.")) {
      "3"
    } else {
      version
    }
  }
}
