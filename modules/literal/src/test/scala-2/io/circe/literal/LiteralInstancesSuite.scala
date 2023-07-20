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

package io.circe.literal

import io.circe.{ Decoder, Encoder }
import munit.FunSuite
import shapeless.Witness

class LiteralInstancesSuite extends FunSuite {
  test("A literal String codec should round-trip values") {
    val w = Witness("foo")

    assertEquals(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor), Right(w.value))
  }

  test("A literal Double codec should round-trip values") {
    val w = Witness(0.0)

    assertEquals(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor), Right(w.value))
  }

  test("A literal Float codec should round-trip values") {
    val w = Witness(0.0f)

    assertEquals(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor), Right(w.value))
  }

  test("A literal Long codec should round-trip values") {
    val w = Witness(0L)

    assertEquals(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor), Right(w.value))
  }

  test("A literal Int codec should round-trip values") {
    val w = Witness(0)

    assertEquals(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor), Right(w.value))
  }

  test("A literal Char codec should round-trip values") {
    val w = Witness('a')

    assertEquals(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor), Right(w.value))
  }

  test("A literal Boolean codec should round-trip values") {
    val w = Witness(true)

    assertEquals(Decoder[w.T].apply(Encoder[w.T].apply(w.value).hcursor), Right(w.value))
  }
}
