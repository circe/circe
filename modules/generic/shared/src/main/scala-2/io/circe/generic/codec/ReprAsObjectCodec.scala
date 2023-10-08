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

package io.circe.generic.codec

import io.circe.DecodingFailure.Reason.WrongTypeExpectation
import io.circe.{ Codec, Decoder, DecodingFailure, HCursor, JsonObject }
import io.circe.generic.Deriver

import shapeless.HNil

/**
 * A codec for a generic representation of a case class or ADT.
 *
 * Note that users typically will not work with instances of this class.
 */
abstract class ReprAsObjectCodec[A] extends Codec.AsObject[A]

object ReprAsObjectCodec {
  implicit def deriveReprAsObjectCodec[R]: ReprAsObjectCodec[R] = macro Deriver.deriveCodec[R]

  val hnilReprCodec: ReprAsObjectCodec[HNil] = new ReprAsObjectCodec[HNil] {
    def apply(c: HCursor): Decoder.Result[HNil] =
      if (c.value.isObject) Right(HNil)
      else Left(DecodingFailure(WrongTypeExpectation("object", c.value), c.history))
    def encodeObject(a: HNil): JsonObject = JsonObject.empty
  }
}
