package io.circe.generic

import io.circe.{ Decoder, DerivationType, Encoder }
import io.circe.`export`.Exported
import scala.deriving.Mirror

/**
 * Fully automatic codec derivation.
 *
 * Extending this trait provides [[io.circe.Decoder]] and [[io.circe.Encoder]]
 * instances for case classes (if all members have instances), sealed
 * trait hierarchies, etc.
 */
trait AutoDerivation {
  implicit inline final def deriveDecoder[A](using inline A: Mirror.Of[A]): Exported[Decoder[A]] =
    Exported(Decoder.derived[A](DerivationType.Auto))
  implicit inline final def deriveEncoder[A](using inline A: Mirror.Of[A]): Exported[Encoder.AsObject[A]] =
    Exported(Encoder.AsObject.derived[A](DerivationType.Auto))
}

object auto extends AutoDerivation
