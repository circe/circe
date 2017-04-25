package io.circe

import _root_.scodec.bits.{ BitVector, ByteVector }

private[circe] trait BitVectorCodec {
  implicit final val decodeBitVector: Decoder[BitVector] =
    Decoder.instance { c =>
      Decoder.decodeJsonObject(c) match {
        case Right(jsonObject) =>
          val decoded = for {
            bitsJ     <- jsonObject("bits")
            bits      <- bitsJ.asString
            lengthJ   <- jsonObject("length")
            length    <- lengthJ.asNumber.flatMap(_.toLong)
            bitVector <- BitVector.fromBase64Descriptive(bits).right.toOption
          } yield bitVector.take(length)

          decoded match {
            case Some(bitVector)  => Right(bitVector)
            case None             => Left(DecodingFailure("Incorrect format of BitVector field", c.history))
          }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[BitVector]]
      }
    }

  /**
    * For serialization of BitVector we use base64. scodec's implementation of `toBase64` adds padding to 8 bits.
    * That's not desired in our case and to preserve original BitVector length we add field "length".
    *
    * Examples:
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
    *
    */
  implicit final val encodeBitVector: Encoder[BitVector] =
    Encoder.encodeJsonObject.contramap { bv =>
      JsonObject.empty
        .add("bits", Json.fromString(bv.toBase64))
        .add("length", Json.fromLong(bv.size))
    }
}

private[circe] trait ByteVectorCodec {
  implicit final val decodeByteVector: Decoder[ByteVector] =
    Decoder.instance { c =>
      Decoder.decodeString(c) match {
        case Right(str) =>
          ByteVector.fromBase64Descriptive(str) match {
            case r @ Right(_) => r.asInstanceOf[Decoder.Result[ByteVector]]
            case Left(err) => Left(DecodingFailure(err, c.history))
          }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[ByteVector]]
      }
    }

  implicit final val encodeByteVector: Encoder[ByteVector] =
    Encoder.encodeString.contramap(_.toBase64)
}

package object scodec extends ByteVectorCodec with BitVectorCodec
