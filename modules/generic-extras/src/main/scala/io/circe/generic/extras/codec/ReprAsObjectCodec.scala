package io.circe.generic.extras.codec

import cats.data.Validated
import io.circe.{ Decoder, DecodingFailure, HCursor, JsonObject }
import io.circe.generic.extras.ConfigurableDeriver
import io.circe.generic.extras.decoding.ReprDecoder
import io.circe.generic.extras.encoding.ReprAsObjectEncoder
import scala.collection.immutable.Map
import scala.language.experimental.macros
import shapeless.HNil

/**
 * A codec for a generic representation of a case class or ADT.
 *
 * Note that users typically will not work with instances of this class.
 */
abstract class ReprAsObjectCodec[A] extends ReprDecoder[A] with ReprAsObjectEncoder[A]

object ReprAsObjectCodec {
  implicit def deriveReprAsObjectCodec[R]: ReprAsObjectCodec[R] = macro ConfigurableDeriver.deriveConfiguredCodec[R]

  val hnilReprCodec: ReprAsObjectCodec[HNil] = new ReprAsObjectCodec[HNil] {
    def configuredDecode(c: HCursor)(
      transformMemberNames: String => String,
      transformConstructorNames: String => String,
      defaults: Map[String, Any],
      discriminator: Option[String]
    ): Decoder.Result[HNil] =
      if (c.value.isObject) Right(HNil) else Left(DecodingFailure("HNil", c.history))

    def configuredDecodeAccumulating(c: HCursor)(
      transformMemberNames: String => String,
      transformConstructorNames: String => String,
      defaults: Map[String, Any],
      discriminator: Option[String]
    ): Decoder.AccumulatingResult[HNil] =
      if (c.value.isObject) Validated.valid(HNil) else Validated.invalidNel(DecodingFailure("HNil", c.history))

    def configuredEncodeObject(a: HNil)(
      transformMemberNames: String => String,
      transformDiscriminator: String => String,
      discriminator: Option[String]
    ): JsonObject = JsonObject.empty
  }
}
