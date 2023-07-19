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

package io.circe.shapes

import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.Encoder
import io.circe.HCursor
import io.circe.Json
import shapeless.:+:
import shapeless.CNil
import shapeless.Coproduct
import shapeless.Inl
import shapeless.Inr

trait CoproductInstances {
  implicit final val decodeCNil: Decoder[CNil] = new Decoder[CNil] {
    def apply(c: HCursor): Decoder.Result[CNil] =
      Left(DecodingFailure("JSON decoding to CNil should never happen", c.history))
  }

  implicit final val encodeCNil: Encoder[CNil] = new Encoder[CNil] {
    def apply(a: CNil): Json = sys.error("Cannot encode CNil")
  }

  implicit final def decodeCCons[L, R <: Coproduct](implicit
    decodeL: Decoder[L],
    decodeR: Decoder[R]
  ): Decoder[L :+: R] = decodeL.map(Inl(_)).or(decodeR.map(Inr(_)))

  implicit final def encodeCCons[L, R <: Coproduct](implicit
    encodeL: Encoder[L],
    encodeR: Encoder[R]
  ): Encoder[L :+: R] = new Encoder[L :+: R] {
    def apply(a: L :+: R): Json = a match {
      case Inl(l) => encodeL(l)
      case Inr(r) => encodeR(r)
    }
  }
}
