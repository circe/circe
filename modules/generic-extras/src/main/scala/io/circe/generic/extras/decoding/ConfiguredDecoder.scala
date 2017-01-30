package io.circe.generic.extras.decoding

import io.circe.{AccumulatingDecoder, Decoder, HCursor}
import io.circe.generic.decoding.DerivedDecoder
import io.circe.generic.extras.{Configuration, Key}
import io.circe.generic.extras.util.{Labelling, RecordToMap}
import shapeless.ops.hlist._

import scala.collection.immutable.Map
import shapeless.{Annotations, Coproduct, Default, HList, LabelledGeneric, Lazy}

abstract class ConfiguredDecoder[A] extends DerivedDecoder[A]

final object ConfiguredDecoder extends IncompleteConfiguredDecoders {
  implicit def decodeCaseClass[A, R <: HList, D <: HList, K <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    decode: Lazy[ReprDecoder[R]],
    defaults: Default.AsRecord.Aux[A, D],
    defaultMapper: RecordToMap[D],
    config: Configuration,
    labels: Labelling.AsList[A],
    keys: Annotations.Aux[Key, A, K],
    toTraversableAuxKeys: ToTraversable.Aux[K, List, Option[Key]]
  ): ConfiguredDecoder[A] = new ConfiguredDecoder[A] {
    private[this] val defaultMap: Map[String, Any] = if (config.useDefaults) defaultMapper(defaults()) else Map.empty

    private[this] val keysAreDefined=keys().toList.flatten.nonEmpty
    @volatile lazy val keysMap:Map[String,String]={
      val fkeys=keys().toList
      labels().map(_.name).zipWithIndex.map{v => Tuple2(v._1, fkeys(v._2))}.filter(_._2.isDefined).map(v=> Tuple2(v._1, v._2.get.value)).toMap
    }

    def keyTransformer(transformKeys: String => String)(value: String): String ={
      keysMap.getOrElse(value, transformKeys(value))
    }

    final def apply(c: HCursor): Decoder.Result[A] = decode.value.configuredDecode(c)(
      if (keysAreDefined) keyTransformer(config.transformKeys) else config.transformKeys,
      defaultMap,
      None
    ) match {
      case Right(r) => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }
    override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[A] =
      decode.value.configuredDecodeAccumulating(c)(
        if (keysAreDefined) keyTransformer(config.transformKeys) else config.transformKeys,
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
