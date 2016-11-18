package io.circe.cursor

import cats.Applicative
import io.circe.{ Cursor, Json }

private[circe] final object CFailure extends Cursor {
  def focus: Json = Json.Null
  def context: List[Either[Int, String]] = Nil
  def up: Cursor = CFailure
  def delete: Cursor = CFailure
  def withFocus(f: Json => Json): Cursor = CFailure
  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[Cursor] = F.pure(CFailure)

  def lefts: Option[List[Json]] = None
  def rights: Option[List[Json]] = None

  def left: Cursor = CFailure
  def right: Cursor = CFailure
  def first: Cursor = CFailure
  def last: Cursor = CFailure

  def deleteGoLeft: Cursor = CFailure
  def deleteGoRight: Cursor = CFailure
  def deleteGoFirst: Cursor = CFailure
  def deleteGoLast: Cursor = CFailure
  def deleteLefts: Cursor = CFailure
  def deleteRights: Cursor = CFailure

  def setLefts(x: List[Json]): Cursor = CFailure
  def setRights(x: List[Json]): Cursor = CFailure

  def field(k: String): Cursor = CFailure
  def deleteGoField(q: String): Cursor = CFailure
}
