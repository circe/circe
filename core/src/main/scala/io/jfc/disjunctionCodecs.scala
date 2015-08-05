package io.jfc

import cats.data.{ Validated, Xor }

/**
 * Provides [[Decoder]] and [[Encoder]] instances for disjunction types with reasonable names for
 * the sides.
 */
object disjunctionCodecs {
  private[this] val leftKey: String = "Left"
  private[this] val rightKey: String = "Right"
  private[this] val failureKey: String = "Invalid"
  private[this] val successKey: String = "Valid"

  implicit def decodeXor[A, B](implicit
    da: Decoder[A],
    db: Decoder[B]
  ): Decoder[Xor[A, B]] = Decoder.decodeXor(leftKey, rightKey)

  implicit def decoderEither[A, B](implicit
    da: Decoder[A],
    db: Decoder[B]
  ): Decoder[Either[A, B]] = Decoder.decodeEither(leftKey, rightKey)

  implicit def decodeValidated[E, A](implicit
    de: Decoder[E],
    da: Decoder[A]
  ): Decoder[Validated[E, A]] = Decoder.decodeValidated(failureKey, successKey)

  implicit def encodeXor[A, B](implicit
    ea: Encoder[A],
    eb: Encoder[B]
  ): Encoder[Xor[A, B]] =
    Encoder.encodeXor(leftKey, rightKey)

  implicit def encodeEither[A, B](implicit
    ea: Encoder[A],
    eb: Encoder[B]
  ): Encoder[Either[A, B]] =
    Encoder.encodeEither(leftKey, rightKey)

  implicit def encodeValidated[E, A](implicit
    ee: Encoder[E],
    ea: Encoder[A]
  ): Encoder[Validated[E, A]] =
    Encoder.encodeValidated(failureKey, successKey)
}
