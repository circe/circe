package io.circe

/**
 * [[Decoder]] and [[Encoder]] instances for disjunction types with reasonable names for the sides.
 */
object disjunctionCodecs {
  private[circe] val leftKey: String = "Left"
  private[circe] val rightKey: String = "Right"

  implicit def decoderEither[A, B](implicit
    da: Decoder[A],
    db: Decoder[B]
  ): Decoder[Either[A, B]] = Decoder.decodeEither(leftKey, rightKey)

  implicit def encodeEither[A, B](implicit
    ea: Encoder[A],
    eb: Encoder[B]
  ): Encoder[Either[A, B]] =
    Encoder.encodeEither(leftKey, rightKey)
}
