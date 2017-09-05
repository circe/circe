package io.circe.generic.extras.encoding

import io.circe.{Encoder, Json}
import shapeless.{::, Generic, HNil, Lazy}

abstract class ValueClassEncoder[A] extends Encoder[A]

final object ValueClassEncoder {
  implicit def encodeValueClass[A <: AnyVal, R](
    implicit
    gen:    Lazy[Generic.Aux[A, R :: HNil]],
    encode: Encoder[R]
  ): ValueClassEncoder[A] = new ValueClassEncoder[A] {
    override def apply(a: A): Json =
      encode(gen.value.to(a).head)
  }
}
