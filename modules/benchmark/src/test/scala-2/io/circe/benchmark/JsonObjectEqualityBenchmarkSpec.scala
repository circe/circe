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

import munit.FunSuite

class JsonObjectEqualityBenchmarkSpec extends FunSuite {
  val benchmark: JsonObjectEqualityBenchmark = new JsonObjectEqualityBenchmark

  test("LinkedHashMap and MapAndVector hashCodes should be identical") {
    assertEquals(benchmark.hashCodeLinkedHashMap, benchmark.hashCodeMapAndVector)
  }

  test("equalsMapAndVector should return true for equivalent JsonObjects") {
    assertEquals(benchmark.equalsMapAndVector, true)
  }

  test("hashCodeMapAndVector should match hash code calculated as in circe 0.14.15") {
    assertEquals(benchmark.hashCodeMapAndVector, benchmark.mapAndVectorObject1.toMap.hashCode())
  }

  test("equalsLinkedHashMap should return true for equivalent JsonObjects") {
    assertEquals(benchmark.equalsLinkedHashMap, true)
  }

  test("hashCodeLinkedHashMap should match hash code calculated as in circe 0.14.15") {
    assertEquals(benchmark.hashCodeLinkedHashMap, benchmark.linkedHashMapObject1.toMap.hashCode())
  }

  test("equalsMixed should return true for differently constructed JsonObjects with identical data") {
    assertEquals(benchmark.equalsMixed, true)
  }
}
