package io.circe.generic.codec

import io.circe.{ Codec, Decoder, HCursor, JsonObject }
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
    def apply(c: HCursor): Decoder.Result[HNil] = Right(HNil)
    def encodeObject(a: HNil): JsonObject = JsonObject.empty
  }
}
