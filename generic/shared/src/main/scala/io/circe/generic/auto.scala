package io.circe.generic

import export.reexports
import io.circe.{ Decoder, Encoder }
import io.circe.generic.decoding.DerivedDecoder
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
  /**
   * Blocks derivation until Shapeless #453 is fixed.
   */
  implicit def decodeObject0: Decoder[Object] = sys.error("No Decoder[Object]")
  implicit def decodeObject1: Decoder[Object] = sys.error("No Decoder[Object]")
  implicit def decodeAnyRef0: Decoder[AnyRef] = sys.error("No Decoder[AnyRef]")
  implicit def decodeAnyRef1: Decoder[AnyRef] = sys.error("No Decoder[AnyRef]")

  implicit def encodeObject0: Encoder[Object] = sys.error("No Encoder[Object]")
  implicit def encodeObject1: Encoder[Object] = sys.error("No Encoder[Object]")
  implicit def encodeAnyRef0: Encoder[AnyRef] = sys.error("No Encoder[AnyRef]")
  implicit def encodeAnyRef1: Encoder[AnyRef] = sys.error("No Encoder[AnyRef]")
}
