package io.circe

import cats.Foldable

class JsonObjectCompanionOps(val underlying: JsonObject.type) extends AnyVal {
  /**
   * Construct a [[JsonObject]] from a foldable collection of key-value pairs.
   */
  def fromFoldable[F[_]](f: F[(String, Json)])(implicit F: Foldable[F]): JsonObject =
    F.foldLeft(f, JsonObject.empty) { case (acc, (k, v)) => acc + (k, v) }
}
