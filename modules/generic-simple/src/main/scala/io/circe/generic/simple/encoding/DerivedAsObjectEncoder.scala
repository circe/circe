package io.circe.generic.simple.encoding

import io.circe.{ Encoder, JsonObject }
import shapeless.{ LabelledGeneric, Lazy }

abstract class DerivedAsObjectEncoder[A] extends Encoder.AsObject[A]

final object DerivedAsObjectEncoder {
  implicit def deriveEncoder[A, R](
    implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[ReprAsObjectEncoder[R]]
  ): DerivedAsObjectEncoder[A] = new DerivedAsObjectEncoder[A] {
    final def encodeObject(a: A): JsonObject = encode.value.encodeObject(gen.to(a))
  }
}
