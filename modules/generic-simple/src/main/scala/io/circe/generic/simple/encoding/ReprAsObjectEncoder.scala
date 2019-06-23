package io.circe.generic.simple.encoding

import io.circe.{ Encoder, JsonObject }
import shapeless.{ :+:, ::, CNil, Coproduct, HList, HNil, Inl, Inr, Lazy, Witness }
import shapeless.labelled.FieldType

/**
 * An encoder for a generic representation of a case class or ADT.
 *
 * Note that users typically will not work with instances of this class.
 */
abstract class ReprAsObjectEncoder[A] extends Encoder.AsObject[A]

final object ReprAsObjectEncoder extends LowPriorityReprAsObjectEncoderInstances {
  implicit val encodeHNil: ReprAsObjectEncoder[HNil] = new ReprAsObjectEncoder[HNil] {
    def encodeObject(a: HNil): JsonObject = JsonObject.empty
  }

  implicit def encodeHCons[K <: Symbol, H, T <: HList](
    implicit
    key: Witness.Aux[K],
    encodeH: Lazy[Encoder[H]],
    encodeT: Lazy[ReprAsObjectEncoder[T]]
  ): ReprAsObjectEncoder[FieldType[K, H] :: T] = new ReprAsObjectEncoder[FieldType[K, H] :: T] {
    def encodeObject(a: FieldType[K, H] :: T): JsonObject = a match {
      case h :: t => ((key.value.name, encodeH.value(h))) +: encodeT.value.encodeObject(t)
    }
  }

  implicit val encodeCNil: ReprAsObjectEncoder[CNil] = new ReprAsObjectEncoder[CNil] {
    def encodeObject(a: CNil): JsonObject =
      sys.error("No JSON representation of CNil (this shouldn't happen)")
  }

  implicit def encodeCoproduct[K <: Symbol, L, R <: Coproduct](
    implicit
    key: Witness.Aux[K],
    encodeL: Lazy[Encoder[L]],
    encodeR: Lazy[ReprAsObjectEncoder[R]]
  ): ReprAsObjectEncoder[FieldType[K, L] :+: R] = new ReprAsObjectEncoder[FieldType[K, L] :+: R] {
    def encodeObject(a: FieldType[K, L] :+: R): JsonObject = a match {
      case Inl(l) => JsonObject.singleton(key.value.name, encodeL.value(l))
      case Inr(r) => encodeR.value.encodeObject(r)
    }
  }
}

private[circe] trait LowPriorityReprAsObjectEncoderInstances {
  implicit def encodeHConsDerived[K <: Symbol, H, T <: HList](
    implicit
    key: Witness.Aux[K],
    encodeH: Lazy[DerivedAsObjectEncoder[H]],
    encodeT: Lazy[ReprAsObjectEncoder[T]]
  ): ReprAsObjectEncoder[FieldType[K, H] :: T] = new ReprAsObjectEncoder[FieldType[K, H] :: T] {
    def encodeObject(a: FieldType[K, H] :: T): JsonObject = a match {
      case h :: t => ((key.value.name, encodeH.value(h))) +: encodeT.value.encodeObject(t)
    }
  }

  implicit def encodeCoproductDerived[K <: Symbol, L, R <: Coproduct](
    implicit
    key: Witness.Aux[K],
    encodeL: Lazy[DerivedAsObjectEncoder[L]],
    encodeR: Lazy[ReprAsObjectEncoder[R]]
  ): ReprAsObjectEncoder[FieldType[K, L] :+: R] = new ReprAsObjectEncoder[FieldType[K, L] :+: R] {
    def encodeObject(a: FieldType[K, L] :+: R): JsonObject = a match {
      case Inl(l) => JsonObject.singleton(key.value.name, encodeL.value(l))
      case Inr(r) => encodeR.value.encodeObject(r)
    }
  }
}
