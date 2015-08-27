package io.circe.generic

import cats.data.Xor
import export.Export
import io.circe.{ Decoder, ObjectEncoder }
import io.circe.generic.decoding.DerivedDecoder
import io.circe.generic.encoding.DerivedObjectEncoder

/**
 * Fully automatic codec derivation.
 *
 * Importing the contents of this object provides [[io.circe.Decoder]] and [[io.circe.Encoder]]
 * instances for tuples, case classes (if all members have instances), "incomplete" case classes,
 * sealed trait hierarchies, etc.
 */
package object auto {
  implicit def decoderExports[T](implicit st: DerivedDecoder[T]): Export[Decoder[T]] =
    decoding.exports[Decoder, T]

  implicit def objectEncoderExports[T](implicit
    st: DerivedObjectEncoder[T]
  ): Export[ObjectEncoder[T]] = encoding.exports[ObjectEncoder, T]
}
