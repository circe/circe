package io.circe.derivation

import scala.deriving.Mirror
import io.circe.{ Codec, Decoder, HCursor, Json }

trait ConfiguredEnumCodec[A] extends Codec[A]
object ConfiguredEnumCodec:
  inline final def derived[A](using conf: Configuration)(using Mirror.SumOf[A]): ConfiguredEnumCodec[A] =
    val decoder = ConfiguredEnumDecoder.derived[A]
    val encoder = ConfiguredEnumEncoder.derived[A]
    new ConfiguredEnumCodec[A]:
      override def apply(c: HCursor): Decoder.Result[A] = decoder(c)
      override def apply(a: A): Json = encoder(a)

  inline final def derive[R: Mirror.SumOf](
    transformConstructorNames: String => String = Configuration.default.transformConstructorNames
  ): Codec[R] = Codec.from(
    ConfiguredEnumDecoder.derive(transformConstructorNames),
    ConfiguredEnumEncoder.derive(transformConstructorNames)
  )
