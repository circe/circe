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

import _root_.scodec.bits.BitVector
import _root_.scodec.bits.ByteVector

package object scodec {
  implicit final val decodeByteVector: Decoder[ByteVector] = Decoder[String].emap(ByteVector.fromBase64Descriptive(_))
  implicit final val encodeByteVector: Encoder[ByteVector] = Encoder.instance(bv => Json.fromString(bv.toBase64))

  implicit final val decodeBitVector: Decoder[BitVector] = decodeBitVectorWithNames("bits", "length")
  implicit final val encodeBitVector: Encoder[BitVector] = encodeBitVectorWithNames("bits", "length")

  final def decodeBitVectorWithNames(bitsName: String, lengthName: String): Decoder[BitVector] =
    Decoder.instance { c =>
      val bits: Decoder.Result[BitVector] = c.get[String](bitsName) match {
        case Right(bs) =>
          BitVector.fromBase64Descriptive(bs) match {
            case r @ Right(_)  => r.asInstanceOf[Decoder.Result[BitVector]]
            case Left(message) => Left(DecodingFailure(message, c.history))
          }
        case Left(err) => Left(err)
      }

      Decoder.resultInstance.map2(bits, c.get[Long](lengthName))(_.take(_))
    }

  /**
   * For serialization of `BitVector` we use base64. scodec's implementation of
   * `toBase64` adds padding to 8 bits. That's not desired in our case and to
   * preserve original BitVector length we add a length field.
   *
   * Examples:
   * {{{
   * encodeBitVector(bin"101")
   * res: io.circe.Json =
   * {
   *   "bits" : "oA==",
   *   "length" : 3
   * }
   *
   * encodeBitVector(bin"")
   * res: io.circe.Json =
   * {
   *   "bits" : "",
   *   "length" : 0
   * }
   *
   * encodeBitVector(bin"11001100")
   * res: io.circe.Json =
   * {
   *   "bits" : "zA==",
   *   "length" : 8
   * }
   * }}}
   */
  final def encodeBitVectorWithNames(bitsName: String, lengthName: String): Encoder.AsObject[BitVector] =
    Encoder.AsObject.instance { bv =>
      JsonObject
        .singleton(bitsName, Json.fromString(bv.toBase64))
        .add(
          lengthName,
          Json.fromLong(bv.size)
        )
    }
}
