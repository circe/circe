package io.circe.generic.encoding

import io.circe.ObjectEncoder

trait DerivedObjectEncoder[A] extends ObjectEncoder[A]

@export.exports
final object DerivedObjectEncoder {
  implicit def upcastConfiguredDerivedObjectEncoder[C, A](implicit
    d: ConfiguredDerivedObjectEncoder[C, A]
  ): DerivedObjectEncoder[A] = d
}
