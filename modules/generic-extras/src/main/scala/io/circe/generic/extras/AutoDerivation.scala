package io.circe.generic.extras

import io.circe.{ Decoder, Encoder }
import io.circe.export.Exported
import io.circe.generic.extras.decoding.ConfiguredDecoder
import io.circe.generic.extras.encoding.ConfiguredAsObjectEncoder
import io.circe.generic.util.macros.ExportMacros
import scala.language.experimental.macros

/**
 * Fully automatic configurable codec derivation.
 *
 * Extending this trait provides [[io.circe.Decoder]] and [[io.circe.Encoder]]
 * instances for case classes (if all members have instances), "incomplete" case classes, sealed
 * trait hierarchies, etc.
 */
trait AutoDerivation {
  implicit def exportDecoder[A]: Exported[Decoder[A]] =
    macro ExportMacros.exportDecoder[ConfiguredDecoder, A]
  implicit def exportEncoder[A]: Exported[Encoder.AsObject[A]] =
    macro ExportMacros.exportEncoder[ConfiguredAsObjectEncoder, A]
}
