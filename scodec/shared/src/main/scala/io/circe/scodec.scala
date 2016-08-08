package io.circe

import _root_.scodec.bits.{BitVector, ByteVector}
import cats.data.Xor

package object scodec {
  implicit final val bitVectorDecoder: Decoder[BitVector] =
    Decoder.instance { c =>
      Decoder.decodeString(c).flatMap { str =>
        BitVector.fromBase64Descriptive(str) match {
          case Right(bits) => Xor.right(bits)
          case Left(err) => Xor.left(DecodingFailure(err, c.history))
        }
      }
    }

  implicit final val bitVectorEncoder: Encoder[BitVector] =
    Encoder.encodeString.contramap(_.toBase64)

  implicit final val byteVectorDecoder: Decoder[ByteVector] =
    Decoder.instance { c =>
      Decoder.decodeString(c).flatMap { str =>
        ByteVector.fromBase64Descriptive(str) match {
          case Right(bytes) => Xor.right(bytes)
          case Left(err) => Xor.left(DecodingFailure(err, c.history))
        }
      }
    }

  implicit final val byteVectorEncoder: Encoder[ByteVector] =
    Encoder.encodeString.contramap(_.toBase64)
}
