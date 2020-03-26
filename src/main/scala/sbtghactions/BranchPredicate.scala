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

sealed trait BranchPredicate extends Product with Serializable

object BranchPredicate {
  final case class Equals(name: String) extends BranchPredicate
  final case class Contains(name: String) extends BranchPredicate
  final case class StartsWith(name: String) extends BranchPredicate
  final case class EndsWith(name: String) extends BranchPredicate
}
