package io.circe.generic.simple.encoding

import io.circe.{ Encoder, JsonObject }
import shapeless.LabelledGeneric

abstract class DerivedAsObjectEncoder[A] extends Encoder.AsObject[A]

object DerivedAsObjectEncoder {
  implicit def deriveEncoder[A, R](
    implicit
    gen: LabelledGeneric.Aux[A, R],
    encodeR: => ReprAsObjectEncoder[R]
  ): DerivedAsObjectEncoder[A] = new DerivedAsObjectEncoder[A] {
    private[this] lazy val cachedEncodeR: Encoder.AsObject[R] = encodeR
    final def encodeObject(a: A): JsonObject = cachedEncodeR.encodeObject(gen.to(a))
  }
}
