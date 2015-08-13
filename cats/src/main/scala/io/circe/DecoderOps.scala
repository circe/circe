package io.circe

import cats.data.{Xor, Kleisli}

class DecoderOps[A](val decoder: Decoder[A]) extends AnyVal {

  /**
   * Convert to a Kleisli arrow.
   */
  def kleisli: Kleisli[({ type L[x] = Xor[DecodingFailure, x] })#L, HCursor, A] =
    Kleisli[({ type L[x] = Xor[DecodingFailure, x] })#L, HCursor, A](decoder(_))

}
