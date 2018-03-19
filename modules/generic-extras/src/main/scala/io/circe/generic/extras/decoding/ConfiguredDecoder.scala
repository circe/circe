package io.circe.generic.extras.decoding

import io.circe.{AccumulatingDecoder, Decoder, HCursor}
import io.circe.generic.decoding.DerivedDecoder
import io.circe.generic.extras.{ Configuration, JsonKey }
import io.circe.generic.extras.util.RecordToMap
import scala.collection.concurrent.TrieMap
import scala.collection.immutable.Map
import shapeless.{ Annotations, Coproduct, Default, HList, LabelledGeneric, Lazy }
import shapeless.ops.hlist.ToTraversable
import shapeless.ops.record.Keys

abstract class ConfiguredDecoder[A] extends DerivedDecoder[A]

final object ConfiguredDecoder extends IncompleteConfiguredDecoders {
  private[this] class CaseClassConfiguredDecoder[A, R <: HList](
    gen: LabelledGeneric.Aux[A, R],
    decodeR: Lazy[ReprDecoder[R]],
    config: Configuration,
    defaultMap: Map[String, Any],
    keyAnnotationMap: Map[String, String]
  ) extends ConfiguredDecoder[A] {

    private[this] val memberNameCache: TrieMap[String, String] = new TrieMap()
    private[this] def memberNameTransformer(value: String): String =
      memberNameCache.getOrElseUpdate(value, {
        if (keyAnnotationMap.nonEmpty)
          keyAnnotationMap.getOrElse(value, config.transformMemberNames(value))
        else
          config.transformMemberNames(value)
      })

    private[this] val constructorNameCache: TrieMap[String, String] = new TrieMap()
    private[this] def constructorNameTransformer(value: String): String =
      constructorNameCache.getOrElseUpdate(value, config.transformConstructorNames(value))

    final def apply(c: HCursor): Decoder.Result[A] = decodeR.value.configuredDecode(c)(
      memberNameTransformer,
      constructorNameTransformer,
      defaultMap,
      None
    ) match {
      case Right(r) => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }

    override final def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      decodeR.value.configuredDecodeAccumulating(c)(
        memberNameTransformer,
        constructorNameTransformer,
        defaultMap,
        None
      ).map(gen.from)
  }

  private[this] class AdtConfiguredDecoder[A, R <: Coproduct](
    gen: LabelledGeneric.Aux[A, R],
    decodeR: Lazy[ReprDecoder[R]],
    config: Configuration
  ) extends ConfiguredDecoder[A] {

    private[this] val constructorNameCache: TrieMap[String, String] = new TrieMap()
    private[this] def constructorNameTransformer(value: String): String =
      constructorNameCache.getOrElseUpdate(value, config.transformConstructorNames(value))

    final def apply(c: HCursor): Decoder.Result[A] = decodeR.value.configuredDecode(c)(
      Predef.identity,
      constructorNameTransformer,
      Map.empty,
      config.discriminator
    ) match {
      case Right(r) => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }

    override final def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      decodeR.value.configuredDecodeAccumulating(c)(
        Predef.identity,
        constructorNameTransformer,
        Map.empty,
        config.discriminator
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
    decodeR: Lazy[ReprDecoder[R]],
    config: Configuration
  ): ConfiguredDecoder[A] = new AdtConfiguredDecoder[A, R](gen, decodeR, config)
}
