package io.circe

import cats.data.Validated

/**
 * [[Decoder]] and [[Encoder]] instances for disjunction types with reasonable names for the sides.
 */
object disjunctionCodecs {
  private[this] final val leftKey: String = "Left"
  private[this] final val rightKey: String = "Right"
  private[this] final val failureKey: String = "Invalid"
  private[this] final val successKey: String = "Valid"

  implicit final def decoderEither[A, B](implicit
    da: Decoder[A],
    db: Decoder[B]
  ): Decoder[Either[A, B]] = Decoder.decodeEither(leftKey, rightKey)

  implicit final def decodeValidated[E, A](implicit
    de: Decoder[E],
    da: Decoder[A]
  ): Decoder[Validated[E, A]] = Decoder.decodeValidated(failureKey, successKey)

  implicit final def encodeEither[A, B](implicit
    ea: Encoder[A],
    eb: Encoder[B]
  ): Encoder[Either[A, B]] =
    Encoder.encodeEither(leftKey, rightKey)

  implicit final def encodeValidated[E, A](implicit
    ee: Encoder[E],
    ea: Encoder[A]
  ): Encoder[Validated[E, A]] =
    Encoder.encodeValidated(failureKey, successKey)
}
