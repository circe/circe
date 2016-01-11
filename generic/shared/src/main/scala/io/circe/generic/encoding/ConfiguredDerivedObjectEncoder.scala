package io.circe.generic.encoding

import io.circe.{ ConfiguredEncoder, ConfiguredObjectEncoder, Encoder, JsonObject, ObjectEncoder }
import io.circe.generic.config.{ SnakeCaseKeys, snakeCase }
import shapeless._, shapeless.labelled.FieldType

trait ConfiguredDerivedObjectEncoder[C, A]
  extends DerivedObjectEncoder[A] with ConfiguredObjectEncoder[C, A]

@export.exports
final object ConfiguredDerivedObjectEncoder extends MidPriorityConfiguredDerivedObjectEncoders {
  implicit final def encodeHNil[C]: ConfiguredDerivedObjectEncoder[C, HNil] =
    new ConfiguredDerivedObjectEncoder[C, HNil] {
      final def encodeObject(a: HNil): JsonObject = JsonObject.empty
    }

  implicit final def encodeCNil[C]: ConfiguredDerivedObjectEncoder[C, CNil] =
    new ConfiguredDerivedObjectEncoder[C, CNil] {
      final def encodeObject(a: CNil): JsonObject =
        sys.error("No JSON representation of CNil (this shouldn't happen)")
    }

  implicit final def encodeCoproduct[C, K <: Symbol, H, T <: Coproduct](implicit
    key: Witness.Aux[K],
    encodeHead: Lazy[Encoder[H]],
    encodeTail: Lazy[ConfiguredDerivedObjectEncoder[C, T]]
  ): ConfiguredDerivedObjectEncoder[C, FieldType[K, H] :+: T] =
    new ConfiguredDerivedObjectEncoder[C, FieldType[K, H] :+: T] {
      final def encodeObject(a: FieldType[K, H] :+: T): JsonObject = a match {
        case Inl(h) => JsonObject.singleton(
          key.value.name,
          encodeHead.value(h)
        )
        case Inr(t) => encodeTail.value.encodeObject(t)
      }
    }

  implicit final def encodeLabelledHListSnakeCaseKeys[K <: Symbol, H, T <: HList](implicit
    key: Witness.Aux[K],
    encodeHead: Lazy[ConfiguredObjectEncoder[SnakeCaseKeys, H]],
    encodeTail: Lazy[ConfiguredDerivedObjectEncoder[SnakeCaseKeys, T]]
  ): ConfiguredDerivedObjectEncoder[SnakeCaseKeys, FieldType[K, H] :: T] =
    new ConfiguredDerivedObjectEncoder[SnakeCaseKeys, FieldType[K, H] :: T] {
      final def encodeObject(a: FieldType[K, H] :: T): JsonObject = a match {
        case h :: t =>
          (snakeCase(key.value.name) -> encodeHead.value(h)) +: encodeTail.value.encodeObject(t)
      }
    }
}

private[circe] trait MidPriorityConfiguredDerivedObjectEncoders
  extends LowPriorityConfiguredDerivedObjectEncoders {
  implicit final def encodeCoproductDerived[C, K <: Symbol, H, T <: Coproduct](implicit
    key: Witness.Aux[K],
    encodeHead: Lazy[ConfiguredDerivedObjectEncoder[C, H]],
    encodeTail: Lazy[ConfiguredDerivedObjectEncoder[C, T]]
  ): ConfiguredDerivedObjectEncoder[C, FieldType[K, H] :+: T] =
    new ConfiguredDerivedObjectEncoder[C, FieldType[K, H] :+: T] {
      final def encodeObject(a: FieldType[K, H] :+: T): JsonObject = a match {
        case Inl(h) => JsonObject.singleton(
          key.value.name,
          encodeHead.value(h)
        )
        case Inr(t) => encodeTail.value.encodeObject(t)
      }
    }

  implicit final def encodeLabelledHListSnakeCaseKeysBase[K <: Symbol, H, T <: HList](implicit
    key: Witness.Aux[K],
    encodeHead: Lazy[Encoder[H]],
    encodeTail: Lazy[ConfiguredDerivedObjectEncoder[SnakeCaseKeys, T]]
  ): ConfiguredDerivedObjectEncoder[SnakeCaseKeys, FieldType[K, H] :: T] =
    new ConfiguredDerivedObjectEncoder[SnakeCaseKeys, FieldType[K, H] :: T] {
      final def encodeObject(a: FieldType[K, H] :: T): JsonObject = a match {
        case h :: t =>
          (snakeCase(key.value.name) -> encodeHead.value(h)) +: encodeTail.value.encodeObject(t)
      }
    }

  implicit final def encodeAdt[C, A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[ConfiguredDerivedObjectEncoder[C, R]]
  ): ConfiguredDerivedObjectEncoder[C, A] =
    new ConfiguredDerivedObjectEncoder[C, A] {
      final def encodeObject(a: A): JsonObject = encode.value.encodeObject(gen.to(a))
    }

  implicit final def encodeCaseClass[C, A, R <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[ConfiguredDerivedObjectEncoder[C, R]]
  ): ConfiguredDerivedObjectEncoder[C, A] =
    new ConfiguredDerivedObjectEncoder[C, A] {
      final def encodeObject(a: A): JsonObject = encode.value.encodeObject(gen.to(a))
    }
}

private[circe] trait LowPriorityConfiguredDerivedObjectEncoders extends
  LowestPriorityConfiguredDerivedObjectEncoders {
  implicit final def encodeLabelledHListUnconfigured[C, K <: Symbol, H, T <: HList](implicit
    key: Witness.Aux[K],
    encodeHead: Lazy[ConfiguredEncoder[C, H]],
    encodeTail: Lazy[ConfiguredDerivedObjectEncoder[C, T]]
  ): ConfiguredDerivedObjectEncoder[C, FieldType[K, H] :: T] =
    new ConfiguredDerivedObjectEncoder[C, FieldType[K, H] :: T] {
      final def encodeObject(a: FieldType[K, H] :: T): JsonObject = a match {
        case h :: t =>
          (key.value.name -> encodeHead.value(h)) +: encodeTail.value.encodeObject(t)
      }
    }
}

private[circe] trait LowestPriorityConfiguredDerivedObjectEncoders {
  implicit final def encodeLabelledHListUnconfiguredBase[C, K <: Symbol, H, T <: HList](implicit
    key: Witness.Aux[K],
    encodeHead: Lazy[Encoder[H]],
    encodeTail: Lazy[ConfiguredDerivedObjectEncoder[C, T]]
  ): ConfiguredDerivedObjectEncoder[C, FieldType[K, H] :: T] =
    new ConfiguredDerivedObjectEncoder[C, FieldType[K, H] :: T] {
      final def encodeObject(a: FieldType[K, H] :: T): JsonObject = a match {
        case h :: t =>
          (key.value.name -> encodeHead.value(h)) +: encodeTail.value.encodeObject(t)
      }
    }
}
