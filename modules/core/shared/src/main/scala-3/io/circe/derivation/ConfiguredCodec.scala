package io.circe.derivation

import scala.deriving.Mirror
import io.circe.{ Codec, Decoder, HCursor, JsonObject }

trait ConfiguredCodec[A] extends Codec.AsObject[A]

object ConfiguredCodec:
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
  inline final def derived[A](using Configuration)(using inline mirror: Mirror.Of[A]): ConfiguredCodec[A] =
    val encoder = ConfiguredEncoder.derived[A]
    val decoder = ConfiguredDecoder.derived[A]
    new ConfiguredCodec[A]:
      final def encodeObject(a: A): JsonObject = encoder.encodeObject(a)
      final def apply(c: HCursor): Decoder.Result[A] = decoder.apply(c)
