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

import munit.FunSuite

class FoldingBenchmarkSpec extends FunSuite {
  val benchmark: FoldingBenchmark = new FoldingBenchmark

  test("withFoldWith should give the correct result") {
    assertEquals(benchmark.withFoldWith, 5463565)
  }

  test("withFold should give the correct result") {
    assertEquals(benchmark.withFold, 5463565)
  }

  test("withPatternMatch should give the correct result") {
    assertEquals(benchmark.withPatternMatch, 5463565)
  }
}
