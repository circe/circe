package io.circe.derivation

import scala.deriving.Mirror
import scala.compiletime.constValue
import io.circe.{ Codec, Decoder, Encoder, HCursor, JsonObject }

trait ConfiguredCodec[A] extends Codec.AsObject[A], ConfiguredDecoder[A], ConfiguredEncoder[A]
object ConfiguredCodec:
  inline final def derived[A](using conf: Configuration)(using mirror: Mirror.Of[A]): ConfiguredCodec[A] =
    val decoder: ConfiguredDecoder[A] = ConfiguredDecoder.derived[A]
    val encoder: ConfiguredEncoder[A] = ConfiguredEncoder.derived[A]
    new ConfiguredCodec[A]:
      val name = constValue[mirror.MirroredLabel]
      lazy val elemLabels: List[String] = decoder.elemLabels
      lazy val elemEncoders: List[Encoder[?]] = encoder.elemEncoders
      lazy val isSum: Boolean = encoder.isSum
      lazy val elemDecoders: List[Decoder[?]] = decoder.elemDecoders
      lazy val elemDefaults: Default[A] = decoder.elemDefaults

      final def encodeObject(a: A): JsonObject = encoder.encodeObject(a)

      final def apply(c: HCursor): Decoder.Result[A] = decoder(c)

      final override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] = decodeAccumulating(c)

  inline final def derive[A: Mirror.Of](
    transformMemberNames: String => String = Configuration.default.transformMemberNames,
    transformConstructorNames: String => String = Configuration.default.transformConstructorNames,
    useDefaults: Boolean = Configuration.default.useDefaults,
    discriminator: Option[String] = Configuration.default.discriminator,
    strictDecoding: Boolean = Configuration.default.strictDecoding
  ): ConfiguredCodec[A] =
    derived[A](using
      Configuration(transformMemberNames, transformConstructorNames, useDefaults, discriminator, strictDecoding)
    )
