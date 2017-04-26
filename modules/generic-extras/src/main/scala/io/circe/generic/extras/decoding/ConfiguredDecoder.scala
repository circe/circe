package io.circe.generic.extras.decoding

import io.circe.{AccumulatingDecoder, Decoder, HCursor}
import io.circe.generic.decoding.DerivedDecoder
import io.circe.generic.extras.{ Configuration, Key }
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
    keys: Annotations.Aux[Key, A, K],
    keysToList: ToTraversable.Aux[K, List, Option[Key]]
  ): ConfiguredDecoder[A] = new ConfiguredDecoder[A] {
    private[this] val defaultMap: Map[String, Any] =
      if (config.useDefaults) defaultMapper(defaults()) else Map.empty

    private[this] val keyAnnotations: List[Option[Key]] = keysToList(keys())
    private[this] val hasKeyAnnotations: Boolean = keyAnnotations.exists(_.nonEmpty)

    private[this] val keyAnnotationMap: Map[String, String] =
      fieldsToList(fields()).map(_.name).zip(keyAnnotations).collect {
        case (field, Some(keyAnnotation)) => (field, keyAnnotation.value)
      }.toMap

    private[this] def keyTransformer(transformKeys: String => String)(value: String): String =
      keyAnnotationMap.getOrElse(value, transformKeys(value))

    final def apply(c: HCursor): Decoder.Result[A] = decode.value.configuredDecode(c)(
      if (hasKeyAnnotations) keyTransformer(config.transformKeys) else config.transformKeys,
      defaultMap,
      None
    ) match {
      case Right(r) => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }

    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      decode.value.configuredDecodeAccumulating(c)(
        if (hasKeyAnnotations) keyTransformer(config.transformKeys) else config.transformKeys,
        defaultMap,
        None
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
      config.discriminator
    ) match {
      case Right(r) => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }

    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      decode.value.configuredDecodeAccumulating(c)(
        Predef.identity,
        Map.empty,
        config.discriminator
      ).map(gen.from)
  }
}
