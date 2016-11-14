package io.circe.generic.extras.encoding

import io.circe.JsonObject
import io.circe.generic.encoding.DerivedObjectEncoder
import io.circe.generic.extras.util.Labelling
import io.circe.generic.extras.{Configuration, Key}
import shapeless.ops.hlist.ToTraversable
import shapeless.{Annotations, Coproduct, HList, LabelledGeneric, Lazy}

abstract class ConfiguredObjectEncoder[A] extends DerivedObjectEncoder[A]

final object ConfiguredObjectEncoder {
  implicit def encodeCaseClass[A, R <: HList, K <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    encode: Lazy[ReprObjectEncoder[R]],
    config: Configuration,
    labels: Labelling.AsList[A],
    keys: Annotations.Aux[Key, A, K],
    toTraversableAuxKeys: ToTraversable.Aux[K, List, Option[Key]]
  ): ConfiguredObjectEncoder[A] = new ConfiguredObjectEncoder[A] {
    private[this] val keysAreDefined=keys().toList.flatten.nonEmpty
    @volatile lazy val keysMap:Map[String,String]={
      val fkeys=keys().toList
      labels().map(_.name).zipWithIndex.map{case (v, p:Int) => v -> fkeys(p)}.filter(_._2.isDefined).map(v=> v._1 -> v._2.get.value).toMap
    }

    def keyTransformer(transformKeys: String => String)(value: String): String ={
      keysMap.getOrElse(value, transformKeys(value))
    }


    final def encodeObject(a: A): JsonObject =
      encode.value.configuredEncodeObject(gen.to(a))(
        if (keysAreDefined) keyTransformer(config.transformKeys) else config.transformKeys,
        None)
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
