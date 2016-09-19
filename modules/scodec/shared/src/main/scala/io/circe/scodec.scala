package io.circe

import _root_.scodec.bits.{BitVector, ByteVector}

package object scodec {
  implicit final val decodeBitVector: Decoder[BitVector] =
    Decoder.instance { c =>
      Decoder.decodeString(c) match {
        case Right(str) =>
          BitVector.fromBase64Descriptive(str) match {
            case r @ Right(_) => r.asInstanceOf[Decoder.Result[BitVector]]
            case Left(err) => Left(DecodingFailure(err, c.history))
          }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[BitVector]]
      }
    }

  implicit final val encodeBitVector: Encoder[BitVector] =
    Encoder.encodeString.contramap(_.toBase64)

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
