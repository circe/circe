/*
 * Copyright 2023 circe
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

package io.circe.testing

import io.circe.DecodingFailure
import io.circe.Json
import io.circe.JsonNumber
import io.circe.JsonObject
import org.scalacheck.Cogen

private[testing] trait CogenInstances {
  implicit val cogenDecodingFailure: Cogen[DecodingFailure] = Cogen((_: DecodingFailure).hashCode.toLong)
  implicit val cogenJson: Cogen[Json] = Cogen((_: Json).hashCode.toLong)
  implicit val cogenJsonNumber: Cogen[JsonNumber] = Cogen((_: JsonNumber).hashCode.toLong)
  implicit val cogenJsonObject: Cogen[JsonObject] = Cogen((_: JsonObject).hashCode.toLong)
}
