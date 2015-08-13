package io.circe

import cats.data.Validated

object catsDisjunctionCodecs {
  import disjunctionCodecs.{ successKey, failureKey }

  implicit def decodeValidated[E, A](implicit
    de: Decoder[E],
    da: Decoder[A]
  ): Decoder[Validated[E, A]] = new Decoders {} .decodeValidated(failureKey, successKey)

  implicit def encodeValidated[E, A](implicit
    ee: Encoder[E],
    ea: Encoder[A]
  ): Encoder[Validated[E, A]] =
    new Encoders {} .encodeValidated(failureKey, successKey)
}
