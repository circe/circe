package io.circe

import _root_.scodec.bits.{ BitVector, ByteVector }

package object scodec {
  implicit final val decodeByteVector: Decoder[ByteVector] = Decoder[String].emap(ByteVector.fromBase64Descriptive(_))
  implicit final val encodeByteVector: Encoder[ByteVector] = Encoder.instance(bv => Json.fromString(bv.toBase64))

  implicit final val decodeBitVector: Decoder[BitVector] = decodeBitVectorWithNames("bits", "length")
  implicit final val encodeBitVector: Encoder[BitVector] = encodeBitVectorWithNames("bits", "length")

  final def decodeBitVectorWithNames(bitsName: String, lengthName: String): Decoder[BitVector] =
    Decoder.instance { c =>
      val bits: Decoder.Result[BitVector] = c.get[String](bitsName).right.flatMap { bs =>
        BitVector.fromBase64Descriptive(bs) match {
          case r @ Right(_)  => r.asInstanceOf[Decoder.Result[BitVector]]
          case Left(message) => Left(DecodingFailure(message, c.history))
        }
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
  final def encodeBitVectorWithNames(bitsName: String, lengthName: String): ObjectEncoder[BitVector] =
    ObjectEncoder.instance { bv =>
      JsonObject
        .singleton(bitsName, Json.fromString(bv.toBase64))
        .add(
          lengthName,
          Json.fromLong(bv.size)
        )
    }
}
