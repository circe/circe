package io.circe.generic

import export.Export
import io.circe.{ Decoder, ObjectEncoder }
import io.circe.generic.decoding.DerivedDecoder
import io.circe.generic.encoding.DerivedObjectEncoder
import shapeless.Lazy

/**
 * Fully automatic codec derivation.
 *
 * Importing the contents of this object provides [[io.circe.Decoder]] and [[io.circe.Encoder]]
 * instances for tuples, case classes (if all members have instances), "incomplete" case classes,
 * sealed trait hierarchies, etc.
 */
package object auto {
  /**
   * Causes the `RecursiveAdtExample` tests in `auto` not to compile.
   *
  implicit def decoderExports[D[x] >: DerivedDecoder[x], T](implicit
  	decode: Lazy[DerivedDecoder[T]]
  ): Export[D[T]] = DerivedDecoder.exports[D, T]

  implicit def objectEncoderExports[E[x] >: DerivedObjectEncoder[x], T](implicit
    encode: Lazy[DerivedObjectEncoder[T]]
  ): Export[E[T]] = DerivedObjectEncoder.exports[E, T]
  */

  import scala.language.experimental.macros

  implicit def decoderExports[D[x] >: DerivedDecoder[x], T]: Export[D[T]] =
    macro export.ExportMacro.exportsImpl0[DerivedDecoder, T, Export]

  implicit def objectEncoderExports[E[x] >: DerivedObjectEncoder[x], T]: Export[E[T]] =
    macro export.ExportMacro.exportsImpl0[DerivedObjectEncoder, T, Export]
}
