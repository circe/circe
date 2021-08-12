package io.circe.derivation

import scala.deriving.Mirror
import scala.compiletime.constValue
import Predef.genericArrayOps
import io.circe.{Encoder, Json, JsonObject}

trait ConfiguredEncoder[A](using conf: Configuration) extends Encoder.AsObject[A], DerivedInstance[A]:
  def elemEncoders: Array[Encoder[_]]

  final def encodeWith(index: Int)(value: Any): (String, Json) =
    (elemLabels(index), elemEncoders(index).asInstanceOf[Encoder[Any]].apply(value))
  
  final def encodedIterable(value: Product): Iterable[(String, Json)] =
    new Iterable[(String, Json)]:
      def iterator: Iterator[(String, Json)] =
        value.productIterator.zipWithIndex.map((value, index) => encodeWith(index)(value))
  
  final def encodeProduct(a: A): JsonObject =
    JsonObject.fromIterable(encodedIterable(a.asInstanceOf[Product]))
  
  final def encodeSum(index: Int, a: A): JsonObject = encodeWith(index)(a) match
    case (k, v) => conf.discriminator match
      case None => JsonObject.singleton(k, v)
      case Some(discriminator) => v.asObject.getOrElse(JsonObject.empty).add(discriminator, Json.fromString(k))

object ConfiguredEncoder:
  inline final def derived[A](using conf: Configuration = Configuration.default)(using mirror: Mirror.Of[A]): ConfiguredEncoder[A] =
    new ConfiguredEncoder[A] with DerivedInstance[A](
      constValue[mirror.MirroredLabel],
      summonLabels[mirror.MirroredElemLabels].map(conf.transformNames).toArray,
    ):
      lazy val elemEncoders: Array[Encoder[_]] = summonEncoders[mirror.MirroredElemTypes].toArray
      
      final def encodeObject(a: A): JsonObject =
        inline mirror match
          case _: Mirror.ProductOf[A] => encodeProduct(a)
          case sum: Mirror.SumOf[A] => encodeSum(sum.ordinal(a), a)
  
  inline final def derive[A: Mirror.Of](
    transformNames: String => String = Configuration.default.transformNames,
    discriminator: Option[String] = Configuration.default.discriminator,
  ): ConfiguredEncoder[A] =
    derived[A](using Configuration(transformNames, useDefaults = false, discriminator))
