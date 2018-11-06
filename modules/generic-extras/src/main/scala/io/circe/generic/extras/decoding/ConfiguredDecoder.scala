package io.circe.generic.extras.decoding

import io.circe.{ AccumulatingDecoder, Decoder, HCursor }
import io.circe.generic.decoding.DerivedDecoder
import io.circe.generic.extras.{ Configuration, JsonKey }
import io.circe.generic.extras.util.RecordToMap
import java.util.concurrent.ConcurrentHashMap
import scala.collection.immutable.Map
import shapeless.{ Annotations, Coproduct, Default, HList, LabelledGeneric, Lazy }
import shapeless.ops.hlist.ToTraversable
import shapeless.ops.record.Keys

abstract class ConfiguredDecoder[A](config: Configuration) extends DerivedDecoder[A] {
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

final object ConfiguredDecoder extends IncompleteConfiguredDecoders {
  private[this] class CaseClassConfiguredDecoder[A, R <: HList](
    gen: LabelledGeneric.Aux[A, R],
    decodeR: Lazy[ReprDecoder[R]],
    config: Configuration,
    defaultMap: Map[String, Any],
    keyAnnotationMap: Map[String, String]
  ) extends ConfiguredDecoder[A](config) {
    private[this] val memberNameCache: ConcurrentHashMap[String, String] =
      new ConcurrentHashMap[String, String]()

    private[this] def memberNameTransformer(value: String): String = {
      val current = memberNameCache.get(value)

      if (current eq null) {
        val transformed = if (keyAnnotationMap.nonEmpty) {
          keyAnnotationMap.getOrElse(value, config.transformMemberNames(value))
        } else {
          config.transformMemberNames(value)
        }

        memberNameCache.put(value, transformed)
        transformed
      } else {
        current
      }
    }

    final def apply(c: HCursor): Decoder.Result[A] = decodeR.value.configuredDecode(c)(
      memberNameTransformer,
      constructorNameTransformer,
      defaultMap,
      None
    ) match {
      case Right(r)    => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }

    override final def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      decodeR.value
        .configuredDecodeAccumulating(c)(
          memberNameTransformer,
          constructorNameTransformer,
          defaultMap,
          None
        )
        .map(gen.from)
  }

  private[this] class AdtConfiguredDecoder[A, R <: Coproduct](
    gen: LabelledGeneric.Aux[A, R],
    decodeR: Lazy[ReprDecoder[R]],
    config: Configuration
  ) extends ConfiguredDecoder[A](config) {
    final def apply(c: HCursor): Decoder.Result[A] = decodeR.value.configuredDecode(c)(
      Predef.identity,
      constructorNameTransformer,
      Map.empty,
      config.discriminator
    ) match {
      case Right(r)    => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }

    override final def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      decodeR.value
        .configuredDecodeAccumulating(c)(
          Predef.identity,
          constructorNameTransformer,
          Map.empty,
          config.discriminator
        )
        .map(gen.from)
  }

  implicit def decodeCaseClass[A, R <: HList, D <: HList, F <: HList, K <: HList](
    implicit
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
      fieldsToList(fields())
        .map(_.name)
        .zip(keysToList(keys()))
        .collect {
          case (field, Some(keyAnnotation)) => (field, keyAnnotation.value)
        }
        .toMap

    new CaseClassConfiguredDecoder[A, R](gen, decodeR.value, config, defaultMap, keyAnnotationMap)
  }

  implicit def decodeAdt[A, R <: Coproduct](
    implicit
    gen: LabelledGeneric.Aux[A, R],
    decodeR: Lazy[ReprDecoder[R]],
    config: Configuration
  ): ConfiguredDecoder[A] = new AdtConfiguredDecoder[A, R](gen, decodeR, config)
}
