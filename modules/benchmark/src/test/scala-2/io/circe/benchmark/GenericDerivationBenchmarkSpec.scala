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
import cats.syntax.eq._

class GenericDerivationBenchmarkSpec extends FunSuite {
  val benchmark: GenericDerivationBenchmark = new GenericDerivationBenchmark

  import benchmark._

  test("The derived codecs should correctly decode Foos") {
    assertEquals(decodeDerived, Right(exampleFoo))
  }

  test("The derived codecs should correctly encode Foos") {
    assertEquals(encodeDerived, exampleFooJson)
  }

  test("The non-derived codecs should correctly decode Foos") {
    assertEquals(decodeNonDerived, Right(exampleFoo))
  }

  test("the non-derived codecs should correctly encode Foos") {
    assertEquals(encodeNonDerived, exampleFooJson)
  }
}
