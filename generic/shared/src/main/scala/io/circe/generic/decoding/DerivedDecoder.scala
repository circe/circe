package io.circe.generic.decoding

import io.circe.Decoder

trait DerivedDecoder[A] extends Decoder[A]

@export.exports
final object DerivedDecoder {
  implicit def upcastConfiguredDerivedDecoder[C, A](implicit
    d: ConfiguredDerivedDecoder[C, A]
  ): DerivedDecoder[A] = d
}
