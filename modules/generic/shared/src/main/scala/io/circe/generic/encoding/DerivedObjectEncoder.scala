package io.circe.generic.encoding

import io.circe.{ JsonObject, ObjectEncoder }
import io.circe.generic.DerivationMacros
import scala.language.experimental.macros
import shapeless.{ Coproduct, HList, LabelledGeneric, Lazy }

abstract class ReprObjectEncoder[A] extends ObjectEncoder[A]

final object ReprObjectEncoder {
  implicit def encodeHList[R <: HList]: ReprObjectEncoder[R] = macro DerivationMacros.encodeHList[R]
  implicit def encodeCoproduct[R <: Coproduct]: ReprObjectEncoder[R] = macro DerivationMacros.encodeCoproduct[R]
}

abstract class DerivedObjectEncoder[A] extends ObjectEncoder[A]

final object DerivedObjectEncoder {
  implicit def encodeCaseClass[A, R <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[ReprObjectEncoder[R]]
  ): DerivedObjectEncoder[A] = new DerivedObjectEncoder[A] {
    final def encodeObject(a: A): JsonObject = encode.value.encodeObject(gen.to(a))
  }

  implicit def encodeAdt[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[ReprObjectEncoder[R]]
  ): DerivedObjectEncoder[A] = new DerivedObjectEncoder[A] {
    final def encodeObject(a: A): JsonObject = encode.value.encodeObject(gen.to(a))
  }
}
