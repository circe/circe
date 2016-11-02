package io.circe.generic.extras.encoding

import io.circe.JsonObject
import io.circe.generic.encoding.DerivedObjectEncoder
import io.circe.generic.extras.Configuration
import shapeless.{ Coproduct, HList, LabelledGeneric, Lazy }

abstract class ConfiguredObjectEncoder[A] extends DerivedObjectEncoder[A]

final object ConfiguredObjectEncoder {
  implicit def encodeCaseClass[A, R <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[ReprObjectEncoder[R]],
    config: Configuration
  ): ConfiguredObjectEncoder[A] = new ConfiguredObjectEncoder[A] {
    final def encodeObject(a: A): JsonObject =
      encode.value.configuredEncodeObject(gen.to(a))(config.transformKeys, None)
  }

  implicit def encodeAdt[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[ReprObjectEncoder[R]],
    config: Configuration
  ): ConfiguredObjectEncoder[A] = new ConfiguredObjectEncoder[A] {
    final def encodeObject(a: A): JsonObject =
      encode.value.configuredEncodeObject(gen.to(a))(identity, config.discriminator)
  }
}
