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

package io.circe.benchmark

import io.circe.Json
import munit.FunSuite

class PointerBenchmarkSpec extends FunSuite {
  val benchmark: PointerBenchmark = new PointerBenchmark

  test("goodOptics should succeed correctly") {
    assertEquals(benchmark.goodOptics, Some(Json.fromInt(123)))
  }

  test("goodPointer should succeed correctly") {
    assertEquals(benchmark.goodPointer, Some(Json.fromInt(123)))
  }

  test("badOptics should fail") {
    assertEquals(benchmark.badOptics, None)
  }

  test("badPointer should fail") {
    assertEquals(benchmark.badPointer, None)
  }
}
