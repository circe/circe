package io.circe.generic

import export.reexports
import io.circe.generic.decoding.{ ConfiguredDerivedDecoder, DerivedDecoder }
import io.circe.generic.encoding.{ ConfiguredDerivedObjectEncoder, DerivedObjectEncoder }

/**
 * Fully automatic codec derivation.
 *
 * Importing the contents of this object provides [[io.circe.Decoder]] and [[io.circe.Encoder]]
 * instances for tuples, case classes (if all members have instances), "incomplete" case classes,
 * sealed trait hierarchies, etc.
 */
@reexports[
  DerivedDecoder,
  ConfiguredDerivedDecoder,
  DerivedObjectEncoder,
  ConfiguredDerivedObjectEncoder
]
final object auto
