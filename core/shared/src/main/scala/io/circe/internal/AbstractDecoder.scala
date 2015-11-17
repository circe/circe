package io.circe.internal

import cats.data.Kleisli
import io.circe.{ HCursor, Json }

trait AbstractDecoder[F[_], A] extends Serializable {
  /**
   * Decode the given hcursor.
   */
  def apply(c: HCursor): F[A]

  /**
   * Decode the given [[Json]] value.
   */
  def decodeJson(j: Json): F[A] = apply(j.cursor.hcursor)

  /**
   * Convert to a Kleisli arrow.
   */
  def kleisli: Kleisli[F, HCursor, A] = Kleisli[F, HCursor, A](apply(_))
}
