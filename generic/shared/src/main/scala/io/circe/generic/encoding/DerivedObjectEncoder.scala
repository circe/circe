package io.circe.generic.encoding

import cats.data.Xor
import export.Exporter0
import io.circe.{ Encoder, Json, JsonObject, ObjectEncoder }
import shapeless._, shapeless.labelled.{ FieldType, field }

trait DerivedObjectEncoder[A] extends ObjectEncoder[A]

object DerivedObjectEncoder extends LowPriorityDerivedObjectEncoders {
  implicit val encodeHNil: DerivedObjectEncoder[HNil] =
    new DerivedObjectEncoder[HNil] {
      def encodeObject(a: HNil): JsonObject = JsonObject.empty
    }

  implicit val encodeCNil: DerivedObjectEncoder[CNil] =
    new DerivedObjectEncoder[CNil] {
      def encodeObject(a: CNil): JsonObject = 
        sys.error("No JSON representation of CNil (this shouldn't happen)")
    }

  implicit def encodeCoproduct[K <: Symbol, H, T <: Coproduct](implicit
    key: Witness.Aux[K],
    encodeHead: Strict[Priority[Encoder[H], DerivedObjectEncoder[H]]],
    encodeTail: Lazy[DerivedObjectEncoder[T]]
  ): DerivedObjectEncoder[FieldType[K, H] :+: T] =
    new DerivedObjectEncoder[FieldType[K, H] :+: T] {
      def encodeObject(a: FieldType[K, H] :+: T): JsonObject = a match {
        case Inl(h) => JsonObject.singleton(
          key.value.name,
          encodeHead.value.fold(identity)(identity)(h)
        )
        case Inr(t) => encodeTail.value.encodeObject(t)
      }
    }

  implicit def encodeLabelledHList[K <: Symbol, H, T <: HList](implicit
    key: Witness.Aux[K],
    encodeHead: Strict[Priority[Encoder[H], DerivedObjectEncoder[H]]],
    encodeTail: Lazy[DerivedObjectEncoder[T]]
  ): DerivedObjectEncoder[FieldType[K, H] :: T] =
    new DerivedObjectEncoder[FieldType[K, H] :: T] {
      def encodeObject(a: FieldType[K, H] :: T): JsonObject = a match {
        case h :: t =>
          (key.value.name -> encodeHead.value.fold(identity)(identity)(h)) +: encodeTail.value.encodeObject(t)
      }
    }
}

trait LowPriorityDerivedObjectEncoders {
  implicit def encodeAdt[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[DerivedObjectEncoder[R]]
  ): DerivedObjectEncoder[A] =
    new DerivedObjectEncoder[A] {
      def encodeObject(a: A): JsonObject = encode.value.encodeObject(gen.to(a))
    }

  implicit def encodeCaseClass[A, R <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[DerivedObjectEncoder[R]]
  ): DerivedObjectEncoder[A] =
    new DerivedObjectEncoder[A] {
      def encodeObject(a: A): JsonObject = encode.value.encodeObject(gen.to(a))
    }
}
