package io.circe.cursor

import cats.Functor
import io.circe.{ Cursor, Json }

private[circe] final case class CJson(focus: Json) extends Cursor {
  def context: List[Either[Int, String]] = Nil
  def up: Option[Cursor] = None
  def delete: Option[Cursor] = None
  def withFocus(f: Json => Json): Cursor = CJson(f(focus))
  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Functor[F]): F[Cursor] =
    F.map(f(focus))(CJson.apply)

  def lefts: Option[List[Json]] = None
  def rights: Option[List[Json]] = None

  def left: Option[Cursor] = None
  def right: Option[Cursor] = None
  def first: Option[Cursor] = None
  def last: Option[Cursor] = None

  def deleteGoLeft: Option[Cursor] = None
  def deleteGoRight: Option[Cursor] = None
  def deleteGoFirst: Option[Cursor] = None
  def deleteGoLast: Option[Cursor] = None
  def deleteLefts: Option[Cursor] = None
  def deleteRights: Option[Cursor] = None

  def setLefts(x: List[Json]): Option[Cursor] = None
  def setRights(x: List[Json]): Option[Cursor] = None

  def field(k: String): Option[Cursor] = None
  def deleteGoField(q: String): Option[Cursor] = None
}
