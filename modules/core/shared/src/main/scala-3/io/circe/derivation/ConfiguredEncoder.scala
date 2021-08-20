package io.circe.derivation

import scala.deriving.Mirror
import scala.compiletime.constValue
import io.circe.{Encoder, Json, JsonObject}

trait ConfiguredEncoder[A](using conf: Configuration) extends Encoder.AsObject[A], DerivedInstance[A]:
  def elemEncoders: Array[Encoder[_]]

  final def encodeElemAt(index: Int, elem: Any): Json =
    elemEncoders(index).asInstanceOf[Encoder[Any]].apply(elem)
  
  final def encodeProduct(a: A): JsonObject =
    val product = a.asInstanceOf[Product]
    val iterable = Iterable.tabulate(product.productArity) { index =>
      val memberName = conf.transformMemberNames(elemLabels(index))
      val json = encodeElemAt(index, product.productElement(index))
      (memberName, json)
    }
    JsonObject.fromIterable(iterable)
  
  final def encodeSum(index: Int, a: A): JsonObject =
    val constructorName = conf.transformConstructorNames(elemLabels(index))
    val json = encodeElemAt(index, a)
    conf.discriminator match
      case None => JsonObject.singleton(constructorName, json)
      case Some(discriminator) => json.asObject.getOrElse(JsonObject.empty).add(discriminator, Json.fromString(constructorName))

object ConfiguredEncoder:
  inline final def derived[A](using conf: Configuration = Configuration.default)(using mirror: Mirror.Of[A]): ConfiguredEncoder[A] =
    new ConfiguredEncoder[A] with DerivedInstance[A](
      constValue[mirror.MirroredLabel],
      summonLabels[mirror.MirroredElemLabels].toArray,
    ):
      lazy val elemEncoders: Array[Encoder[_]] = summonEncoders[mirror.MirroredElemTypes].toArray
      
      final def encodeObject(a: A): JsonObject =
        inline mirror match
          case _: Mirror.ProductOf[A] => encodeProduct(a)
          case sum: Mirror.SumOf[A] => encodeSum(sum.ordinal(a), a)
  
  inline final def derive[A: Mirror.Of](
    transformMemberNames: String => String = Configuration.default.transformMemberNames,
    transformConstructorNames: String => String = Configuration.default.transformConstructorNames,
    discriminator: Option[String] = Configuration.default.discriminator,
  ): ConfiguredEncoder[A] =
    derived[A](using Configuration(transformMemberNames, transformConstructorNames, useDefaults = false, discriminator))
