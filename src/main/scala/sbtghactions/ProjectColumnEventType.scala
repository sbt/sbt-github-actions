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

/**
 * @see https://docs.github.com/en/developers/webhooks-and-events/webhook-events-and-payloads#project_column
 */

sealed trait ProjectColumnEventType extends EventType

object ProjectColumnEventType {
  case object Created extends ProjectColumnEventType
  case object Updated extends ProjectColumnEventType
  case object Moved   extends ProjectColumnEventType
  case object Deleted extends ProjectColumnEventType
}
