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

import syntax.*
import io.circe.tests.CirceMunitSuite

class FromProductSuite extends CirceMunitSuite:

  test("encode correctly Example(id: Int, value: String)") {
    case class Example(id: Int, value: String)

    given Encoder[Example] = Encoder.forTupleProduct2("id", "value")(Tuple.fromProductTyped[Example])
    given Decoder[Example] = Decoder.forProduct2("id", "value")(Example.apply)

    val example = Example(1, "hello")

    val encoded = example.asJson
    val decoded = encoded.as[Example]

    assertEquals(decoded, Right(example))
  }

  test("codec encode/decode correctly Example(id: Int, value: String)") {
    case class Example(id: Int, value: String)

    given Codec[Example] = Codec.forTupleProduct2("id", "value")(Example.apply)(Tuple.fromProductTyped[Example])

    val example = Example(1, "hello")

    val encoded = example.asJson
    val decoded = encoded.as[Example]

    assertEquals(decoded, Right(example))
  }
