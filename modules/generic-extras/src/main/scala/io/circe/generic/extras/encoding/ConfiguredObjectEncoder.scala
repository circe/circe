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

    private[this] val transformedMemberCache: Map[String, String] = {
      fieldsToList(fields()).map(f => (f.name, memberNameTransformer(f.name))).toMap
    }

    private[this] def transformMemberName(value: String) =
      transformedMemberCache.getOrElse(value, value)

    private[this] val constructorNameCache: TrieMap[String, String] = TrieMap()
    private[this] def constructorNameTransformer(value: String): String =
      constructorNameCache.getOrElseUpdate(value, config.transformConstructorNames(value))

    final def encodeObject(a: A): JsonObject =
      encode.value.configuredEncodeObject(gen.to(a))(
        transformMemberName,
        constructorNameTransformer,
        None
      )
  }

  implicit def encodeAdt[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[ReprObjectEncoder[R]],
    config: Configuration
  ): ConfiguredObjectEncoder[A] = new ConfiguredObjectEncoder[A] {

    private[this] val constructorNameCache: TrieMap[String, String] = TrieMap()
    private[this] def constructorNameTransformer(value: String): String =
      constructorNameCache.getOrElseUpdate(value, config.transformConstructorNames(value))

    final def encodeObject(a: A): JsonObject =
      encode.value.configuredEncodeObject(gen.to(a))(
        Predef.identity,
        constructorNameTransformer,
        config.discriminator
      )
  }
}
