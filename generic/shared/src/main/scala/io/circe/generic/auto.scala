package io.circe.generic

import io.circe.generic.decoding.{ DerivedConfiguredDecoder, DerivedDecoder }
import io.circe.generic.encoding.{ DerivedConfiguredEncoder, DerivedEncoder }

/**
 * Fully automatic codec derivation.
 *
 * Importing the contents of this object provides [[io.circe.Decoder]] and [[io.circe.Encoder]]
 * instances for tuples, case classes (if all members have instances), "incomplete" case classes,
 * sealed trait hierarchies, etc.
 */
@export.reexports[
  DerivedDecoder,
  DerivedConfiguredDecoder,
  DerivedEncoder,
  DerivedConfiguredEncoder
]
final object auto
