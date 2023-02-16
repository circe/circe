package io.circe.derivation

import scala.deriving.Mirror
import scala.compiletime.constValue
import io.circe.{ Encoder, Json, JsonObject }

trait ConfiguredEncoder[A](using conf: Configuration) extends Encoder.AsObject[A]:
  lazy val elemLabels: List[String]
  lazy val elemEncoders: List[Encoder[?]]
  lazy val isSum: Boolean

  final def encodeElemAt(index: Int, elem: Any, transformName: String => String): (String, Json) = {
    (transformName(elemLabels(index)), elemEncoders(index).asInstanceOf[Encoder[Any]].apply(elem))
  }

  final def encodeProduct(a: A): JsonObject =
    val product = a.asInstanceOf[Product]
    val iterable = Iterable.tabulate(product.productArity) { index =>
      encodeElemAt(index, product.productElement(index), conf.transformMemberNames)
    }
    JsonObject.fromIterable(iterable)

  final def encodeSum(index: Int, a: A): JsonObject =
    val (constructorName, json) = encodeElemAt(index, a, conf.transformConstructorNames)

    val isConcrete = elemEncoders(index) match {
      case ce: ConfiguredEncoder[?] => !ce.isSum
      case _                        => true
    }

    conf.discriminator match
      case Some(discriminator) =>
        val jo = json.asObject.getOrElse(JsonObject.empty)
        if (isConcrete)
          jo.add(discriminator, Json.fromString(constructorName))
        else jo
      case None => JsonObject.singleton(constructorName, json)

object ConfiguredEncoder:
  inline final def derived[A](using conf: Configuration)(using inline mirror: Mirror.Of[A]): ConfiguredEncoder[A] =
    new ConfiguredEncoder[A]:
      lazy val elemLabels: List[String] = summonLabels[mirror.MirroredElemLabels]
      lazy val elemEncoders: List[Encoder[?]] = summonEncoders[mirror.MirroredElemTypes]
      lazy val isSum: Boolean =
        inline mirror match
          case _: Mirror.ProductOf[A] => false
          case _: Mirror.SumOf[A]     => true

      final def encodeObject(a: A): JsonObject =
        inline mirror match
          case _: Mirror.ProductOf[A] => encodeProduct(a)
          case sum: Mirror.SumOf[A]   => encodeSum(sum.ordinal(a), a)

  inline final def derive[A: Mirror.Of](
    transformMemberNames: String => String = Configuration.default.transformMemberNames,
    transformConstructorNames: String => String = Configuration.default.transformConstructorNames,
    discriminator: Option[String] = Configuration.default.discriminator
  ): ConfiguredEncoder[A] =
    derived[A](using Configuration(transformMemberNames, transformConstructorNames, useDefaults = false, discriminator))
