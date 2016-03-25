package io.circe.generic

import io.circe.{ Decoder, ObjectEncoder }
import io.circe.export.Exported
import scala.language.experimental.macros

/**
 * Fully automatic codec derivation.
 *
 * Importing the contents of this object provides [[io.circe.Decoder]] and [[io.circe.Encoder]]
 * instances for tuples, case classes (if all members have instances), "incomplete" case classes,
 * sealed trait hierarchies, etc.
 */
final object auto {
  implicit def exportDecoder[A]: Exported[Decoder[A]] = macro DerivationMacros.exportDecoderImpl[A]
  implicit def exportEncoder[A]: Exported[ObjectEncoder[A]] = macro DerivationMacros.exportEncoderImpl[A]
}
