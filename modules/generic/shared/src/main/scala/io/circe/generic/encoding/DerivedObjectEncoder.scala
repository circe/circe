package io.circe.generic.encoding

import io.circe.{ JsonObject, ObjectEncoder }
import shapeless.{ LabelledGeneric, Lazy }

abstract class DerivedObjectEncoder[A] extends ObjectEncoder[A]

final object DerivedObjectEncoder {
  implicit def deriveEncoder[A, R](implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[ReprObjectEncoder[R]]
  ): DerivedObjectEncoder[A] = new DerivedObjectEncoder[A] {
    final def encodeObject(a: A): JsonObject = encode.value.encodeObject(gen.to(a))
  }
}
