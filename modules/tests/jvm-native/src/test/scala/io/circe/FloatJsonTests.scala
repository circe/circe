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

package io.circe

import cats.syntax.eq._
import io.circe.tests.CirceMunitSuite

/**
 * Tests that fail because of limitations on Scala.js.
 */
trait FloatJsonTests { this: CirceMunitSuite =>
  test("fromFloatOrString should return a Json number for valid Floats") {
    assertEquals(Json.fromFloatOrString(1.1f), Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe("1.1")))
    assertEquals(Json.fromFloatOrString(-1.2f), Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe("-1.2")))
  }
}
