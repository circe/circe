package io.circe.generic.encoding

import io.circe.Encoder
import io.circe.generic.Deriver
import scala.language.experimental.macros

/**
 * An encoder for a generic representation of a case class or ADT.
 *
 * Note that users typically will not work with instances of this class.
 */
abstract class ReprAsObjectEncoder[A] extends Encoder.AsObject[A]

object ReprAsObjectEncoder {
  implicit def deriveReprAsObjectEncoder[R]: ReprAsObjectEncoder[R] = macro Deriver.deriveEncoder[R]
}
