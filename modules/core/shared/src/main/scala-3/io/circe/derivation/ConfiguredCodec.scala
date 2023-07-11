package io.circe.derivation

import scala.deriving.Mirror
import scala.compiletime.constValue
import io.circe.{ Codec, Decoder, Encoder, HCursor, JsonObject }

trait ConfiguredCodec[A] extends Codec.AsObject[A], ConfiguredDecoder[A], ConfiguredEncoder[A]
object ConfiguredCodec:

  inline final def derived[A](using conf: Configuration)(using
    inline mirror: Mirror.Of[A]
  ): ConfiguredCodec[A] =
    new ConfiguredCodec[A] with SumOrProduct:
      val name = constValue[mirror.MirroredLabel]
      lazy val elemLabels: List[String] = summonLabels[mirror.MirroredElemLabels]
      lazy val elemEncoders: List[Encoder[?]] = summonEncoders[mirror.MirroredElemTypes]
      lazy val elemDecoders: List[Decoder[?]] = summonDecoders[mirror.MirroredElemTypes]
      lazy val elemDefaults: Default[A] = Predef.summon[Default[A]]
      lazy val isSum: Boolean =
        inline mirror match
          case _: Mirror.ProductOf[A] => false
          case _: Mirror.SumOf[A]     => true

      final def encodeObject(a: A): JsonObject =
        inline mirror match
          case _: Mirror.ProductOf[A] => encodeProduct(a)
          case sum: Mirror.SumOf[A]   => encodeSum(sum.ordinal(a), a)

      final def apply(c: HCursor): Decoder.Result[A] =
        inline mirror match
          case product: Mirror.ProductOf[A] => decodeProduct(c, product.fromProduct)
          case _: Mirror.SumOf[A]           => decodeSum(c)

      final override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
        inline mirror match
          case product: Mirror.ProductOf[A] => decodeProductAccumulating(c, product.fromProduct)
          case _: Mirror.SumOf[A]           => decodeSumAccumulating(c)

  inline final def derive[A: Mirror.Of](
    transformMemberNames: String => String = Configuration.default.transformMemberNames,
    transformConstructorNames: String => String = Configuration.default.transformConstructorNames,
    useDefaults: Boolean = Configuration.default.useDefaults,
    discriminator: Option[String] = Configuration.default.discriminator,
    strictDecoding: Boolean = Configuration.default.strictDecoding,
    autoRecursiveDerivation: Boolean = Configuration.default.autoRecursiveDerivation
  ): ConfiguredCodec[A] =
    derived[A](using
      Configuration(
        transformMemberNames,
        transformConstructorNames,
        useDefaults,
        discriminator,
        strictDecoding,
        autoRecursiveDerivation
      )
    )
