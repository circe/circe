package io.circe.generic

import io.circe.{ Decoder, ObjectEncoder }
import io.circe.export.Exported
import io.circe.generic.decoding.DerivedDecoder
import io.circe.generic.encoding.DerivedObjectEncoder
import io.circe.generic.util.macros.ExportMacros
import scala.language.experimental.macros

/**
 * Fully automatic codec derivation.
 *
 * Extending this trait provides [[io.circe.Decoder]] and [[io.circe.Encoder]]
 * instances for case classes (if all members have instances), "incomplete" case classes, sealed
 * trait hierarchies, etc.
 */
trait AutoDerivation {
  implicit def exportDecoder[A]: Exported[Decoder[A]] = macro ExportMacros.exportDecoder[DerivedDecoder, A]
  implicit def exportEncoder[A]: Exported[ObjectEncoder[A]] = macro ExportMacros.exportEncoder[DerivedObjectEncoder, A]
}
