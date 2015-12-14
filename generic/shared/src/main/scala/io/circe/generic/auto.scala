package io.circe.generic

import export.reexports
import io.circe.generic.decoding.{ DerivedDecoder, DerivedDecoderWithDefaults }
import io.circe.generic.encoding.DerivedObjectEncoder

/**
 * Fully automatic codec derivation.
 *
 * Importing the contents of this object provides [[io.circe.Decoder]] and [[io.circe.Encoder]]
 * instances for tuples, case classes (if all members have instances), "incomplete" case classes,
 * sealed trait hierarchies, etc.
 */
@reexports[DerivedDecoder, DerivedObjectEncoder]
object auto {
  @reexports[DerivedDecoderWithDefaults, DerivedObjectEncoder]
  object defaults
}
