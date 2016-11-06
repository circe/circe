package io.circe.generic.encoding

import io.circe.ObjectEncoder
import io.circe.generic.Deriver
import scala.language.experimental.macros

/**
 * An encoder for a generic representation of a case class or ADT.
 *
 * Note that users typically will not work with instances of this class.
 */
abstract class ReprObjectEncoder[A] extends ObjectEncoder[A]

final object ReprObjectEncoder {
  implicit def deriveReprObjectEncoder[R]: ReprObjectEncoder[R] = macro Deriver.deriveEncoder[R]
}
