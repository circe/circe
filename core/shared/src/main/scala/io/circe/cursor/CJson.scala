package io.circe.cursor

import cats.Functor
import io.circe.{ Context, Cursor, Json }

private[circe] case class CJson(focus: Json) extends Cursor {
  def context: List[Context] = Nil
  def up: Option[Cursor] = None
  def delete: Option[Cursor] = None
  def withFocus(f: Json => Json): Cursor = CJson(f(focus))
  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Functor[F]): F[Cursor] =
    F.map(f(focus))(CJson.apply)
}
