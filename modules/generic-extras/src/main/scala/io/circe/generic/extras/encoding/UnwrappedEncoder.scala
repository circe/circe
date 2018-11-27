package io.circe.generic.extras.encoding

import io.circe.{ Encoder, Json }
import shapeless.{ ::, Generic, HNil, Lazy }

abstract class UnwrappedEncoder[A] extends Encoder[A]

final object UnwrappedEncoder {
  implicit def encodeUnwrapped[A <: AnyVal, R](
    implicit
    gen: Lazy[Generic.Aux[A, R :: HNil]],
    encode: Encoder[R]
  ): UnwrappedEncoder[A] = new UnwrappedEncoder[A] {
    override def apply(a: A): Json =
      encode(gen.value.to(a).head)
  }
}
