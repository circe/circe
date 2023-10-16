package io.circe.generic.simple

import io.circe.{ Decoder, Encoder }
import io.circe.export.Exported
import io.circe.generic.simple.decoding.DerivedDecoder
import io.circe.generic.simple.encoding.DerivedAsObjectEncoder
import io.circe.generic.simple.util.macros.ExportMacros

/**
 * Fully automatic codec derivation.
 *
 * Extending this trait provides [[io.circe.Decoder]] and [[io.circe.Encoder]]
 * instances for case classes (if all members have instances), "incomplete" case classes, sealed
 * trait hierarchies, etc.
 */
trait AutoDerivation {
  implicit def exportDecoder[A]: Exported[Decoder[A]] = macro ExportMacros.exportDecoder[DerivedDecoder, A]
  implicit def exportEncoder[A]: Exported[Encoder.AsObject[A]] =
    macro ExportMacros.exportEncoder[DerivedAsObjectEncoder, A]
}
