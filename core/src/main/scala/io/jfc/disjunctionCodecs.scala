package io.jfc

import cats.data.{ Validated, Xor }

/**
 * Provides [[Decode]] and [[Encode]] instances for disjunction types with reasonable names for the
 * sides.
 */
object disjunctionCodecs {
  private[this] val leftKey: String = "Left"
  private[this] val rightKey: String = "Right"
  private[this] val failureKey: String = "Invalid"
  private[this] val successKey: String = "Valid"

  implicit def decodeXor[A, B](implicit
    da: Decode[A],
    db: Decode[B]
  ): Decode[Xor[A, B]] = Decode.decodeXor(leftKey, rightKey)

  implicit def decodeEither[A, B](implicit
    da: Decode[A],
    db: Decode[B]
  ): Decode[Either[A, B]] = Decode.decodeEither(leftKey, rightKey)

  implicit def decodeValidated[E, A](implicit
    de: Decode[E],
    da: Decode[A]
  ): Decode[Validated[E, A]] = Decode.decodeValidated(failureKey, successKey)

  implicit def encodeXor[A, B](implicit
    ea: Encode[A],
    eb: Encode[B]
  ): Encode[Xor[A, B]] =
    Encode.encodeXor(leftKey, rightKey)

  implicit def encodeEither[A, B](implicit
    ea: Encode[A],
    eb: Encode[B]
  ): Encode[Either[A, B]] =
    Encode.encodeEither(leftKey, rightKey)

  implicit def encodeValidated[E, A](implicit
    ee: Encode[E],
    ea: Encode[A]
  ): Encode[Validated[E, A]] =
    Encode.encodeValidated(failureKey, successKey)
}
