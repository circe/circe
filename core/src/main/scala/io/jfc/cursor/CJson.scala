package io.jfc.cursor

import cats.Functor
import io.jfc.{ Context, Cursor, Json }

private[jfc] case class CJson(focus: Json) extends Cursor {
  def context: Context = Context.empty
  def up: Option[Cursor] = None
  def delete: Option[Cursor] = None
  def withFocus(f: Json => Json): Cursor = CJson(f(focus))
  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Functor[F]): F[Cursor] =
    F.map(f(focus))(CJson.apply)
}
