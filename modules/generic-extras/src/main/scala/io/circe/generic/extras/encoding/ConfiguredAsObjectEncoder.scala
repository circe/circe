package io.circe.generic.extras.encoding

import io.circe.JsonObject
import io.circe.generic.encoding.DerivedAsObjectEncoder
import io.circe.generic.extras.{ Configuration, JsonKey }
import java.util.concurrent.ConcurrentHashMap
import scala.collection.immutable.Map
import shapeless.{ Annotations, Coproduct, HList, LabelledGeneric, Lazy }
import shapeless.ops.hlist.ToTraversable
import shapeless.ops.record.Keys

abstract class ConfiguredAsObjectEncoder[A](config: Configuration) extends DerivedAsObjectEncoder[A] {
  private[this] val constructorNameCache: ConcurrentHashMap[String, String] =
    new ConcurrentHashMap[String, String]()

  protected[this] def constructorNameTransformer(value: String): String = {
    val current = constructorNameCache.get(value)

    if (current eq null) {
      val transformed = config.transformConstructorNames(value)
      constructorNameCache.put(value, transformed)
      transformed
    } else {
      current
    }
  }
}

final object ConfiguredAsObjectEncoder {
  implicit def encodeCaseClass[A, R <: HList, F <: HList, K <: HList](
    implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[ReprAsObjectEncoder[R]],
    config: Configuration,
    fields: Keys.Aux[R, F],
    fieldsToList: ToTraversable.Aux[F, List, Symbol],
    keys: Annotations.Aux[JsonKey, A, K],
    keysToList: ToTraversable.Aux[K, List, Option[JsonKey]]
  ): ConfiguredAsObjectEncoder[A] = new ConfiguredAsObjectEncoder[A](config) {
    private[this] val keyAnnotations: List[Option[JsonKey]] = keysToList(keys())
    private[this] val hasKeyAnnotations: Boolean = keyAnnotations.exists(_.nonEmpty)

    private[this] val keyAnnotationMap: Map[String, String] =
      fieldsToList(fields())
        .map(_.name)
        .zip(keyAnnotations)
        .collect {
          case (field, Some(keyAnnotation)) => (field, keyAnnotation.value)
        }
        .toMap

    private[this] def memberNameTransformer(value: String): String =
      if (hasKeyAnnotations)
        keyAnnotationMap.getOrElse(value, config.transformMemberNames(value))
      else
        config.transformMemberNames(value)

    private[this] val transformedMemberCache: Map[String, String] = {
      fieldsToList(fields()).map(f => (f.name, memberNameTransformer(f.name))).toMap
    }

    private[this] def transformMemberName(value: String) =
      transformedMemberCache.getOrElse(value, value)

    final def encodeObject(a: A): JsonObject =
      encode.value.configuredEncodeObject(gen.to(a))(
        transformMemberName,
        constructorNameTransformer,
        None
      )
  }

  implicit def encodeAdt[A, R <: Coproduct](
    implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[ReprAsObjectEncoder[R]],
    config: Configuration
  ): ConfiguredAsObjectEncoder[A] = new ConfiguredAsObjectEncoder[A](config) {
    final def encodeObject(a: A): JsonObject =
      encode.value.configuredEncodeObject(gen.to(a))(
        Predef.identity,
        constructorNameTransformer,
        config.discriminator
      )
  }
}
