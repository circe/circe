package io.circe.generic.simple.encoding

import io.circe.{ Encoder, JsonObject }
import shapeless.LabelledGeneric

abstract class DerivedAsObjectEncoder[A] extends Encoder.AsObject[A]

final object DerivedAsObjectEncoder {
  implicit def deriveEncoder[A, R](
    implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: => ReprAsObjectEncoder[R]
  ): DerivedAsObjectEncoder[A] = new DerivedAsObjectEncoder[A] {
    final def encodeObject(a: A): JsonObject = encode.encodeObject(gen.to(a))
  }
}
