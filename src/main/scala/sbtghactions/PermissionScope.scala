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

sealed trait Permissions extends Product with Serializable

/**
 * @see https://docs.github.com/en/actions/using-jobs/assigning-permissions-to-jobs#overview
 */
object Permissions {
  case object ReadAll extends Permissions
  case object WriteAll extends Permissions
  case object None extends Permissions
  final case class Specify(values: Map[PermissionScope, PermissionValue]) extends Permissions
}

sealed trait PermissionScope extends Product with Serializable

object PermissionScope {
  case object Actions extends PermissionScope
  case object Checks extends PermissionScope
  case object Contents extends PermissionScope
  case object Deployments extends PermissionScope
  case object IdToken extends PermissionScope
  case object Issues extends PermissionScope
  case object Discussions extends PermissionScope
  case object Packages extends PermissionScope
  case object Pages extends PermissionScope
  case object PullRequests extends PermissionScope
  case object RepositoryProjects extends PermissionScope
  case object SecurityEvents extends PermissionScope
  case object Statuses extends PermissionScope
}

sealed trait PermissionValue extends Product with Serializable

object PermissionValue {
  case object Read extends PermissionValue
  case object Write extends PermissionValue
  case object None extends PermissionValue
}
