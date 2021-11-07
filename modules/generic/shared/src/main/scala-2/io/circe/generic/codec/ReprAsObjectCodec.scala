package io.circe.generic.codec

import io.circe.DecodingFailure.Reason.WrongTypeExpectation
import io.circe.{ Codec, Decoder, DecodingFailure, HCursor, JsonObject }
import io.circe.generic.Deriver

import scala.language.experimental.macros
import shapeless.HNil

/**
 * A codec for a generic representation of a case class or ADT.
 *
 * Note that users typically will not work with instances of this class.
 */
abstract class ReprAsObjectCodec[A] extends Codec.AsObject[A]

object ReprAsObjectCodec {
  implicit def deriveReprAsObjectCodec[R]: ReprAsObjectCodec[R] = macro Deriver.deriveCodec[R]

  val hnilReprCodec: ReprAsObjectCodec[HNil] = new ReprAsObjectCodec[HNil] {
    def apply(c: HCursor): Decoder.Result[HNil] =
      if (c.value.isObject) Right(HNil)
      else Left(DecodingFailure(WrongTypeExpectation("object", c.value), c.history))
    def encodeObject(a: HNil): JsonObject = JsonObject.empty
  }
}
