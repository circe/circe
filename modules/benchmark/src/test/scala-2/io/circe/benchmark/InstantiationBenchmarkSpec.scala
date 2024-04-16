/*
 * Copyright 2024 circe
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

package io.circe.benchmark

import io.circe.Json
import munit.FunSuite
class InstantiationBenchmarkSpec extends FunSuite {
  val benchmark: InstantiationBenchmark = new InstantiationBenchmark

  import benchmark._

  test("decoderFromNew should correctly decode") {
    assertEquals(decoderFromNew, Right("xyz"))
  }

  test("decoderFromSAM should correctly decode") {
    assertEquals(decoderFromNew, Right("xyz"))
  }

  test("decoderFromInstance should correctly decode") {
    assertEquals(decoderFromNew, Right("xyz"))
  }

  test("encoderFromNew should correctly encode") {
    assertEquals(encoderFromNew, Json.obj("value" -> Json.fromString("abc")))
  }

  test("encoderFromSAM should correctly encode") {
    assertEquals(encoderFromSAM, Json.obj("value" -> Json.fromString("abc")))
  }

  test("encoderFromInstance should correctly encode") {
    assertEquals(encoderFromInstance, Json.obj("value" -> Json.fromString("abc")))
  }
}
