package io.circe.generic.extras.decoding

import io.circe.{Decoder, HCursor}
import shapeless.{::, Generic, HNil, Lazy}

abstract class ValueClassDecoder[A] extends Decoder[A]

final object ValueClassDecoder {
  implicit def decodeValueClass[A <: AnyVal, R](
    implicit
    gen:    Lazy[Generic.Aux[A, R :: HNil]],
    decode: Decoder[R]
  ): ValueClassDecoder[A] = new ValueClassDecoder[A] {
    override def apply(c: HCursor): Decoder.Result[A] =
      decode(c).map { value ⇒
        gen.value.from(value :: HNil)
      }
  }
}
