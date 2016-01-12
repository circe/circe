package io.circe.generic.encoding

import io.circe.{ ConfiguredEncoder, Encoder, Json, JsonObject }
import io.circe.generic.NotExported
import io.circe.generic.config._
import shapeless._, shapeless.labelled.FieldType

trait DerivedEncoder[A] extends Encoder[A]

@export.exports
final object DerivedEncoder {
  implicit final def fromDerivedConfiguredEncoder[A](implicit
    encoder: DerivedConfiguredEncoder[Unit, A]
  ): DerivedEncoder[A] = encoder
}

trait DerivedConfiguredEncoder[C, A] extends ConfiguredEncoder[C, A] with DerivedEncoder[A]

@export.exports
final object DerivedConfiguredEncoder {
  implicit final def deriveConfiguredEncoder[C, A](implicit
    encoder: Lazy[ConfigurableEncoder[C, A]],
    config: Configuration[C]
  ): DerivedConfiguredEncoder[C, A] = encoder.value(config)
}

trait ConfigurableEncoder[C, A] {
  def apply(config: Configuration[C]): DerivedConfiguredEncoder[C, A]
}

final object ConfigurableEncoder extends LowPriorityConfigurableEncoders {
  private[this] final def unconfigurable[C, A](encoder: DerivedConfiguredEncoder[C, A]): ConfigurableEncoder[C, A] =
    new ConfigurableEncoder[C, A] {
      final def apply(config: Configuration[C]): DerivedConfiguredEncoder[C, A] = encoder
    }

  final implicit def encodeHNil[C, A]: ConfigurableEncoder[C, HNil] = unconfigurable(
    new DerivedConfiguredEncoder[C, HNil] {
      final def apply(a: HNil): Json = Json.JObject(JsonObject.empty)
    }
  )

  implicit final def encodeCNil[C, A]: ConfigurableEncoder[C, CNil] = unconfigurable(
    new DerivedConfiguredEncoder[C, CNil] {
      final def apply(a: CNil): Json = sys.error("No JSON representation of CNil (this shouldn't happen)")
    }
  )

  implicit final def encodeCaseClass[C, A, R <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    encoder: Lazy[ConfigurableEncoder[C, R]]
  ): ConfigurableEncoder[C, A] = new ConfigurableEncoder[C, A] {
    final def apply(config: Configuration[C]): DerivedConfiguredEncoder[C, A] =
      new DerivedConfiguredEncoder[C, A] {
        final def apply(a: A): Json = encoder.value(config)(gen.to(a))
      }
  }

  implicit final def encodeAdt[C, A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    encoder: Lazy[ConfigurableEncoder[C, R]]
  ): ConfigurableEncoder[C, A] = new ConfigurableEncoder[C, A] {
    final def apply(config: Configuration[C]): DerivedConfiguredEncoder[C, A] =
      new DerivedConfiguredEncoder[C, A] {
        final def apply(a: A): Json = encoder.value(config)(gen.to(a))
      }
  }

  implicit final def encodeLabelledHList[C, K <: Symbol, H, T <: HList](implicit
    name: Witness.Aux[K],
    headEncoder: NotExported[Encoder[H]],
    tailEncoder: Lazy[ConfigurableEncoder[C, T]]
  ): ConfigurableEncoder[C, FieldType[K, H] :: T] =
    new ConfigurableEncoder[C, FieldType[K, H] :: T] {
      final def apply(config: Configuration[C]): DerivedConfiguredEncoder[C, FieldType[K, H] :: T] =
        new HListEncoder[C, K, H, T](
          name.value.name,
          headEncoder.value,
          tailEncoder.value(config),
          config
        )
    }

  implicit final def encodeCoproduct[C, K <: Symbol, H, T <: Coproduct](implicit
    name: Witness.Aux[K],
    headEncoder: NotExported[Encoder[H]],
    tailEncoder: Lazy[ConfigurableEncoder[C, T]]
  ): ConfigurableEncoder[C, FieldType[K, H] :+: T] =
    new ConfigurableEncoder[C, FieldType[K, H] :+: T] {
      final def apply(config: Configuration[C]): DerivedConfiguredEncoder[C, FieldType[K, H] :+: T] =
        new CoproductEncoder[C, K, H, T](
          name.value.name,
          headEncoder.value,
          tailEncoder.value(config),
          config
        )
    }
}

private[circe] trait LowPriorityConfigurableEncoders {
  implicit final def encodeLabelledHListRec[C, K <: Symbol, H, T <: HList](implicit
    name: Witness.Aux[K],
    headEncoder: Lazy[ConfigurableEncoder[C, H]],
    tailEncoder: Lazy[ConfigurableEncoder[C, T]]
  ): ConfigurableEncoder[C, FieldType[K, H] :: T] =
    new ConfigurableEncoder[C, FieldType[K, H] :: T] {
      final def apply(config: Configuration[C]): DerivedConfiguredEncoder[C, FieldType[K, H] :: T] =
        new HListEncoder[C, K, H, T](
          name.value.name,
          headEncoder.value(config),
          tailEncoder.value(config),
          config
        )
    }

  implicit final def encodeCoproductRec[C, K <: Symbol, H, T <: Coproduct](implicit
    name: Witness.Aux[K],
    headEncoder: Lazy[ConfigurableEncoder[C, H]],
    tailEncoder: Lazy[ConfigurableEncoder[C, T]]
  ): ConfigurableEncoder[C, FieldType[K, H] :+: T] =
    new ConfigurableEncoder[C, FieldType[K, H] :+: T] {
      final def apply(config: Configuration[C]): DerivedConfiguredEncoder[C, FieldType[K, H] :+: T] =
        new CoproductEncoder[C, K, H, T](
          name.value.name,
          headEncoder.value(config),
          tailEncoder.value(config),
          config
        )
    }
}

private[generic] class CoproductEncoder[C, K <: Symbol, H, T <: Coproduct](
  name: String,
  headEncoder: Encoder[H],
  tailEncoder: Encoder[T],
  config: Configuration[C]
) extends DerivedConfiguredEncoder[C, FieldType[K, H] :+: T] {
  final def apply(a: FieldType[K, H] :+: T): Json = a match {
    case Inl(h) => headEncoder(h) match {
      case Json.JObject(obj) if obj.isEmpty && config.caseObjectEncoding == CaseObjectString =>
        Json.string(name)
      case headJson =>
        config.discriminator match {
          case ObjectWrapper => Json.JObject(JsonObject.singleton(name, headJson))
          case DiscriminatorKey(key) => headJson match {
            case Json.JObject(obj) => Json.JObject((key -> Json.string(name)) +: obj)
            case other => Json.JObject(JsonObject.singleton(name, other))
          }
      }
    }
    case Inr(t) => tailEncoder(t)
  }
}

private[generic] class HListEncoder[C, K <: Symbol, H, T <: HList](
  name: String,
  headEncoder: Encoder[H],
  tailEncoder: Encoder[T],
  config: Configuration[C]
) extends DerivedConfiguredEncoder[C, FieldType[K, H] :: T] {
  final def apply(a: FieldType[K, H] :: T): Json = a match {
    case h :: t => tailEncoder(t) match {
      case Json.JObject(obj) =>
        Json.JObject((config.keyTransformation(name) -> headEncoder(h)) +: obj)
      case _ => sys.error("Impossible (TODO: Encode this in the type system)")
    }
  }
}
