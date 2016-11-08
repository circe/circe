package io.circe.shapeless

import io.circe.{ Decoder, DecodingFailure, Encoder, HCursor, Json }
import shapeless.{ :+:, CNil, Coproduct, Inl, Inr }

trait CoproductInstances {
  implicit final val decodeCNil: Decoder[CNil] = new Decoder[CNil] {
    def apply(c: HCursor): Decoder.Result[CNil] = Left(DecodingFailure("CNil", c.history))
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
