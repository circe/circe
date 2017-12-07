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
  private[this] class CaseClassConfiguredDecoder[A, R <: HList](
    gen: LabelledGeneric.Aux[A, R],
    decodeR: ReprDecoder[R],
    config: Configuration,
    defaultMap: Map[String, Any],
    keyAnnotationMap: Map[String, String]
  ) extends ConfiguredDecoder[A] {
    private[this] def memberNameTransformer(transformMemberNames: String => String)(value: String): String =
      keyAnnotationMap.getOrElse(value, transformMemberNames(value))

    final def apply(c: HCursor): Decoder.Result[A] = decodeR.configuredDecode(c)(
      if (keyAnnotationMap.nonEmpty) {
        memberNameTransformer(config.transformMemberNames)
      } else {
        config.transformMemberNames
      },
      config.transformConstructorNames,
      defaultMap,
      None
    ) match {
      case Right(r) => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }

    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      decodeR.configuredDecodeAccumulating(c)(
        if (keyAnnotationMap.nonEmpty) {
          memberNameTransformer(config.transformMemberNames)
        } else {
          config.transformMemberNames
        },
        config.transformConstructorNames,
        defaultMap,
        None
      ).map(gen.from)
  }

  implicit def decodeCaseClass[A, R <: HList, D <: HList, F <: HList, K <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    decodeR: Lazy[ReprDecoder[R]],
    defaults: Default.AsRecord.Aux[A, D],
    defaultMapper: RecordToMap[D],
    config: Configuration,
    fields: Keys.Aux[R, F],
    fieldsToList: ToTraversable.Aux[F, List, Symbol],
    keys: Annotations.Aux[JsonKey, A, K],
    keysToList: ToTraversable.Aux[K, List, Option[JsonKey]]
  ): ConfiguredDecoder[A] = {
    val defaultMap: Map[String, Any] =
      if (config.useDefaults) defaultMapper(defaults()) else Map.empty

    val keyAnnotationMap: Map[String, String] =
      fieldsToList(fields()).map(_.name).zip(keysToList(keys())).collect {
        case (field, Some(keyAnnotation)) => (field, keyAnnotation.value)
      }.toMap

    new CaseClassConfiguredDecoder[A, R](gen, decodeR.value, config, defaultMap, keyAnnotationMap)
  }

  implicit def decodeAdt[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    decode: Lazy[ReprDecoder[R]],
    config: Configuration
  ): ConfiguredDecoder[A] = new ConfiguredDecoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] = decode.value.configuredDecode(c)(
      Predef.identity,
      config.transformConstructorNames,
      Map.empty,
      config.discriminator
    ) match {
      case Right(r) => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }

    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      decode.value.configuredDecodeAccumulating(c)(
        Predef.identity,
        config.transformConstructorNames,
        Map.empty,
        config.discriminator
      ).map(gen.from)
  }
}
