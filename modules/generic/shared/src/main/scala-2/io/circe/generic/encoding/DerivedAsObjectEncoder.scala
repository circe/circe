package io.circe.generic.encoding

import io.circe.{ Encoder, Json, JsonObject }
import shapeless.{ LabelledGeneric, Lazy }

abstract class DerivedAsObjectEncoder[A] extends Encoder.AsObject[A]

object DerivedAsObjectEncoder {
  implicit def deriveEncoder[A, R](implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[ReprAsObjectEncoder[R]]
  ): DerivedAsObjectEncoder[A] = new DerivedAsObjectEncoder[A] {
    final def encodeObject(a: A): JsonObject[Json] = encode.value.encodeObject(gen.to(a))
  }
}
