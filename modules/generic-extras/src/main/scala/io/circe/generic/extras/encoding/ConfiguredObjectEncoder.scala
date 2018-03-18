package io.circe.generic.extras.encoding

import io.circe.JsonObject
import io.circe.generic.encoding.DerivedObjectEncoder
import io.circe.generic.extras.{ Configuration, JsonKey }
import scala.collection.concurrent.TrieMap
import scala.collection.immutable.Map
import shapeless.{ Annotations, Coproduct, HList, LabelledGeneric, Lazy }
import shapeless.ops.hlist.ToTraversable
import shapeless.ops.record.Keys

abstract class ConfiguredObjectEncoder[A] extends DerivedObjectEncoder[A]

final object ConfiguredObjectEncoder {
  implicit def encodeCaseClass[A, R <: HList, F <: HList, K <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[ReprObjectEncoder[R]],
    config: Configuration,
    fields: Keys.Aux[R, F],
    fieldsToList: ToTraversable.Aux[F, List, Symbol],
    keys: Annotations.Aux[JsonKey, A, K],
    keysToList: ToTraversable.Aux[K, List, Option[JsonKey]]
  ): ConfiguredObjectEncoder[A] = new ConfiguredObjectEncoder[A] {
    private[this] val keyAnnotations: List[Option[JsonKey]] = keysToList(keys())
    private[this] val hasKeyAnnotations: Boolean = keyAnnotations.exists(_.nonEmpty)

    private[this] val keyAnnotationMap: Map[String, String] =
      fieldsToList(fields()).map(_.name).zip(keyAnnotations).collect {
        case (field, Some(keyAnnotation)) => (field, keyAnnotation.value)
      }.toMap

    private[this] def memberNameTransformer(value: String): String = {
      if (hasKeyAnnotations)
        keyAnnotationMap.getOrElse(value, config.transformMemberNames(value))
      else
        config.transformMemberNames(value)
    }

    private[this] val transformedMemberNames: Map[String, String] = {
      fieldsToList(fields()).map(f => (f.name, memberNameTransformer(f.name))).toMap
    }

    private[this] val constructorNames: TrieMap[String, String] = TrieMap()

    final def encodeObject(a: A): JsonObject =
      encode.value.configuredEncodeObject(gen.to(a))(
        v => transformedMemberNames.getOrElse(v, v),
        v => constructorNames.getOrElseUpdate(v, config.transformConstructorNames(v)),
        None
      )
  }

  implicit def encodeAdt[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[ReprObjectEncoder[R]],
    config: Configuration
  ): ConfiguredObjectEncoder[A] = new ConfiguredObjectEncoder[A] {

    private[this] val constructorNames: TrieMap[String, String] = TrieMap()

    final def encodeObject(a: A): JsonObject =
      encode.value.configuredEncodeObject(gen.to(a))(
        Predef.identity,
        v => constructorNames.getOrElseUpdate(v, config.transformConstructorNames(v)),
        config.discriminator
      )
  }
}
