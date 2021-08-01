package io.circe.derivation

import scala.deriving.Mirror
import scala.compiletime.constValue
import Predef.genericArrayOps
import io.circe.{Decoder, Encoder, JsonObject, Codec, HCursor}
import io.circe.syntax._

trait ConfiguredCodec[A] extends Codec.AsObject[A]
object ConfiguredCodec {
  inline final def derived[A](using conf: Configuration = Configuration.default)(using inline A: Mirror.Of[A]): ConfiguredCodec[A] =
    new ConfiguredCodec[A]
        with ConfiguredDecoder[A]
        with ConfiguredEncoder[A]
        with DerivedInstance[A](
          constValue[A.MirroredLabel],
          summonLabels[A.MirroredElemLabels].map(conf.transformNames).toArray,
        ) {
      lazy val elemEncoders: Array[Encoder[_]] = summonEncoders[A.MirroredElemTypes].toArray
      lazy val elemDecoders: Array[Decoder[_]] = summonDecoders[A.MirroredElemTypes].toArray
      lazy val elemDefaults: Default[A] = Predef.summon[Default[A]]

      final def encodeObject(a: A): JsonObject = inline A match {
        case m: Mirror.ProductOf[A] => encodeProduct(a)
        case m: Mirror.SumOf[A] => encodeSum(m.ordinal(a), a)
      }
      
      final def apply(c: HCursor): Decoder.Result[A] =
        inline A match {
          case m: Mirror.ProductOf[A] => decodeProduct(c, m.fromProduct)
          case m: Mirror.SumOf[A] => decodeSum(c)
        }
      
      final override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
        inline A match {
          case m: Mirror.ProductOf[A] => decodeProductAccumulating(c, m.fromProduct)
          case m: Mirror.SumOf[A] => decodeSumAccumulating(c)
        }
  }

  inline final def derive[A: Mirror.Of](
    transformNames: String => String = Configuration.default.transformNames,
    useDefaults: Boolean = Configuration.default.useDefaults,
    discriminator: Option[String] = Configuration.default.discriminator,
  ): ConfiguredCodec[A] = {
    given Configuration = Configuration(transformNames, useDefaults, discriminator)
    derived[A]
  }
}
