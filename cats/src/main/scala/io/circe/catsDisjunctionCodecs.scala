package io.circe

import cats.data.{Xor, Validated}

object catsDisjunctionCodecs {
  import disjunctionCodecs.{ leftKey, rightKey }

  private[this] val failureKey: String = "Invalid"
  private[this] val successKey: String = "Valid"

  implicit def decodeXor[A, B](implicit
    da: Decoder[A],
    db: Decoder[B]
  ): Decoder[Xor[A, B]] = new Decoders {} .decodeXor(leftKey, rightKey)

  implicit def decodeValidated[E, A](implicit
    de: Decoder[E],
    da: Decoder[A]
  ): Decoder[Validated[E, A]] = new Decoders {} .decodeValidated(failureKey, successKey)

  implicit def encodeXor[A, B](implicit
    ea: Encoder[A],
    eb: Encoder[B]
  ): Encoder[Xor[A, B]] =
    new Encoders {} .encodeXor(leftKey, rightKey)

  implicit def encodeValidated[E, A](implicit
    ee: Encoder[E],
    ea: Encoder[A]
  ): Encoder[Validated[E, A]] =
    new Encoders {} .encodeValidated(failureKey, successKey)
}
