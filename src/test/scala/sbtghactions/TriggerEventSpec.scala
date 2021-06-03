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

import org.specs2.mutable.Specification
import sbtghactions.ManualEvents.Input

class TriggerEventSpec extends Specification {
  "workflow dispatch" should {
    "render without inputs" in {
      ManualEvents.WorkflowDispatch(Nil).render mustEqual "workflow_dispatch:\n"
    }

    "render inputs" in {

      val expected =
        """workflow_dispatch:
          |  inputs:
          |    ref:
          |      description: The branch, tag or SHA to build
          |      required: true
          |      default: master""".stripMargin

      ManualEvents.WorkflowDispatch(
        List(
          Input("ref", "The branch, tag or SHA to build", Some("master"), required = true)
          )
        ).render mustEqual expected
    }
  }

  "repository dispatch" should {
    "render without types" in {
      ManualEvents.RepositoryDispatch(Nil).render mustEqual "repository_dispatch:\n"
    }

    "render types" in {

      val expected =
        """repository_dispatch:
          |  types: [event1, event2]""".stripMargin

      ManualEvents.RepositoryDispatch(List("event1", "event2")).render mustEqual expected
    }
  }

}
