package io.circe.generic.extras.encoding

import io.circe.{ Encoder, Json }
import shapeless.{ ::, Generic, HNil, Lazy }

abstract class UnwrappedEncoder[A] extends Encoder[A]

object UnwrappedEncoder {
  implicit def encodeUnwrapped[A, R](
    implicit
    gen: Lazy[Generic.Aux[A, R :: HNil]],
    encodeR: Encoder[R]
  ): UnwrappedEncoder[A] = new UnwrappedEncoder[A] {
    override def apply(a: A): Json =
      encodeR(gen.value.to(a).head)
  }
}
