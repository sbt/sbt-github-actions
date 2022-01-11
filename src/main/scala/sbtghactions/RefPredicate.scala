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

sealed trait RefPredicate extends Product with Serializable

object RefPredicate {
  final case class Equals(ref: Ref) extends RefPredicate
  final case class Contains(ref: Ref) extends RefPredicate
  final case class StartsWith(ref: Ref) extends RefPredicate
  final case class EndsWith(ref: Ref) extends RefPredicate
}
