package io.circe.derivation

import scala.deriving.Mirror
import io.circe.{Decoder, Codec, HCursor, Json}

trait ConfiguredEnumCodec[A] extends Codec[A]
object ConfiguredEnumCodec:
  inline final def derived[A](using conf: EnumConfiguration = EnumConfiguration.default)(using Mirror.SumOf[A]): ConfiguredEnumCodec[A] =
    val decoder = ConfiguredEnumDecoder.derived[A]
    val encoder = ConfiguredEnumEncoder.derived[A]
    new ConfiguredEnumCodec[A]:
      override def apply(c: HCursor): Decoder.Result[A] = decoder(c)
      override def apply(a: A): Json = encoder(a)

  inline final def derive[R: Mirror.SumOf](
    decoderTransform: String => String = EnumConfiguration.default.decodeTransformNames,
    encoderTransform: String => String = EnumConfiguration.default.encodeTransformNames,
  ): Codec[R] = Codec.from(
    ConfiguredEnumDecoder.derive(decoderTransform),
    ConfiguredEnumEncoder.derive(encoderTransform),
  )
