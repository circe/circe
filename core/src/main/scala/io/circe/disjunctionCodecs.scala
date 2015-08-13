package io.circe

import cats.data.Xor

/**
 * [[Decoder]] and [[Encoder]] instances for disjunction types with reasonable names for the sides.
 */
object disjunctionCodecs {
  private[this] val leftKey: String = "Left"
  private[this] val rightKey: String = "Right"
  private[circe] val failureKey: String = "Invalid"
  private[circe] val successKey: String = "Valid"

  implicit def decodeXor[A, B](implicit
    da: Decoder[A],
    db: Decoder[B]
  ): Decoder[Xor[A, B]] = Decoder.decodeXor(leftKey, rightKey)

  implicit def decoderEither[A, B](implicit
    da: Decoder[A],
    db: Decoder[B]
  ): Decoder[Either[A, B]] = Decoder.decodeEither(leftKey, rightKey)

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
}
