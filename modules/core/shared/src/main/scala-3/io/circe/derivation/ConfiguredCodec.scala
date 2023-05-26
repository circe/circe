package io.circe.derivation

import scala.deriving.Mirror
import io.circe.{ Codec, Decoder, HCursor, JsonObject }

trait ConfiguredCodec[A] extends Codec.AsObject[A]
object ConfiguredCodec:
  inline final def derived[A](using Configuration)(using inline mirror: Mirror.Of[A]): ConfiguredCodec[A] =
    val encoder = ConfiguredEncoder.derived[A]
    val decoder = ConfiguredDecoder.derived[A]
    new ConfiguredCodec[A]:
      final def encodeObject(a: A): JsonObject = encoder.encodeObject(a)
      final def apply(c: HCursor): Decoder.Result[A] = decoder.apply(c)
