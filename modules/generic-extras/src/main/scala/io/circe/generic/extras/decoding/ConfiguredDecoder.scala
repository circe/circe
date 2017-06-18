package io.circe.generic.extras.decoding

import io.circe.{AccumulatingDecoder, Decoder, HCursor}
import io.circe.generic.decoding.DerivedDecoder
import io.circe.generic.extras.{ Configuration, JsonKey }
import io.circe.generic.extras.util.RecordToMap
import scala.collection.immutable.Map
import shapeless.{ Annotations, Coproduct, Default, HList, LabelledGeneric, Lazy }
import shapeless.ops.hlist.ToTraversable
import shapeless.ops.record.Keys

abstract class ConfiguredDecoder[A] extends DerivedDecoder[A]

final object ConfiguredDecoder extends IncompleteConfiguredDecoders {
  implicit def decodeCaseClass[A, R <: HList, D <: HList, F <: HList, K <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    decode: Lazy[ReprDecoder[R]],
    defaults: Default.AsRecord.Aux[A, D],
    defaultMapper: RecordToMap[D],
    config: Configuration,
    fields: Keys.Aux[R, F],
    fieldsToList: ToTraversable.Aux[F, List, Symbol],
    keys: Annotations.Aux[JsonKey, A, K],
    keysToList: ToTraversable.Aux[K, List, Option[JsonKey]]
  ): ConfiguredDecoder[A] = new ConfiguredDecoder[A] {
    private[this] val defaultMap: Map[String, Any] =
      if (config.useDefaults) defaultMapper(defaults()) else Map.empty

    private[this] val keyAnnotations: List[Option[JsonKey]] = keysToList(keys())
    private[this] val hasKeyAnnotations: Boolean = keyAnnotations.exists(_.nonEmpty)

    private[this] val keyAnnotationMap: Map[String, String] =
      fieldsToList(fields()).map(_.name).zip(keyAnnotations).collect {
        case (field, Some(keyAnnotation)) => (field, keyAnnotation.value)
      }.toMap

    private[this] def memberNameTransformer(transformMemberNames: String => String)(value: String): String =
      keyAnnotationMap.getOrElse(value, transformMemberNames(value))

    final def apply(c: HCursor): Decoder.Result[A] = decode.value.configuredDecode(c)(
      if (hasKeyAnnotations) memberNameTransformer(config.transformMemberNames) else config.transformMemberNames,
      defaultMap,
      None,
      config.transformConstructorNames
    ) match {
      case Right(r) => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }

    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      decode.value.configuredDecodeAccumulating(c)(
        if (hasKeyAnnotations) memberNameTransformer(config.transformMemberNames) else config.transformMemberNames,
        defaultMap,
        None,
        config.transformConstructorNames
      ).map(gen.from)
  }

  implicit def decodeAdt[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    decode: Lazy[ReprDecoder[R]],
    config: Configuration
  ): ConfiguredDecoder[A] = new ConfiguredDecoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] = decode.value.configuredDecode(c)(
      Predef.identity,
      Map.empty,
      config.discriminator,
      config.transformConstructorNames
    ) match {
      case Right(r) => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }

    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      decode.value.configuredDecodeAccumulating(c)(
        Predef.identity,
        Map.empty,
        config.discriminator,
        config.transformConstructorNames
      ).map(gen.from)
  }
}
