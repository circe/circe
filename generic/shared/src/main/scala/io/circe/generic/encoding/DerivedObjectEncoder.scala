package io.circe.generic.encoding

import io.circe.{ Encoder, JsonObject, ObjectEncoder }
import shapeless._, shapeless.labelled.FieldType

trait DerivedObjectEncoder[A] extends ObjectEncoder[A]

@export.exports
final object DerivedObjectEncoder extends LowPriorityDerivedObjectEncoders {
  implicit final val encodeHNil: DerivedObjectEncoder[HNil] =
    new DerivedObjectEncoder[HNil] {
      final def encodeObject(a: HNil): JsonObject = JsonObject.empty
    }

  implicit final val encodeCNil: DerivedObjectEncoder[CNil] =
    new DerivedObjectEncoder[CNil] {
      final def encodeObject(a: CNil): JsonObject =
        sys.error("No JSON representation of CNil (this shouldn't happen)")
    }

  implicit final def encodeCoproduct[K <: Symbol, H, T <: Coproduct](implicit
    key: Witness.Aux[K],
    encodeHead: Lazy[Encoder[H]],
    encodeTail: Lazy[DerivedObjectEncoder[T]]
  ): DerivedObjectEncoder[FieldType[K, H] :+: T] =
    new DerivedObjectEncoder[FieldType[K, H] :+: T] {
      final def encodeObject(a: FieldType[K, H] :+: T): JsonObject = a match {
        case Inl(h) => JsonObject.singleton(
          key.value.name,
          encodeHead.value(h)
        )
        case Inr(t) => encodeTail.value.encodeObject(t)
      }
    }

  implicit final def encodeLabelledHList[K <: Symbol, H, T <: HList](implicit
    key: Witness.Aux[K],
    encodeHead: Lazy[Encoder[H]],
    encodeTail: Lazy[DerivedObjectEncoder[T]]
  ): DerivedObjectEncoder[FieldType[K, H] :: T] =
    new DerivedObjectEncoder[FieldType[K, H] :: T] {
      final def encodeObject(a: FieldType[K, H] :: T): JsonObject = a match {
        case h :: t =>
          (key.value.name -> encodeHead.value(h)) +: encodeTail.value.encodeObject(t)
      }
    }
}

private[circe] trait LowPriorityDerivedObjectEncoders {
  implicit final def encodeCoproductDerived[K <: Symbol, H, T <: Coproduct](implicit
    key: Witness.Aux[K],
    encodeHead: Lazy[DerivedObjectEncoder[H]],
    encodeTail: Lazy[DerivedObjectEncoder[T]]
  ): DerivedObjectEncoder[FieldType[K, H] :+: T] =
    new DerivedObjectEncoder[FieldType[K, H] :+: T] {
      final def encodeObject(a: FieldType[K, H] :+: T): JsonObject = a match {
        case Inl(h) => JsonObject.singleton(
          key.value.name,
          encodeHead.value(h)
        )
        case Inr(t) => encodeTail.value.encodeObject(t)
      }
    }

  implicit final def encodeAdt[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[DerivedObjectEncoder[R]]
  ): DerivedObjectEncoder[A] =
    new DerivedObjectEncoder[A] {
      final def encodeObject(a: A): JsonObject = encode.value.encodeObject(gen.to(a))
    }

  implicit final def encodeCaseClass[A, R <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[DerivedObjectEncoder[R]]
  ): DerivedObjectEncoder[A] =
    new DerivedObjectEncoder[A] {
      final def encodeObject(a: A): JsonObject = encode.value.encodeObject(gen.to(a))
    }
}
