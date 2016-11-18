package io.circe.cursor

import cats.Applicative
import io.circe.{ Cursor, Json, JsonObject }

private[circe] final case class CObject(
  focus: Json,
  key: String,
  parent: Cursor,
  changed: Boolean,
  obj: JsonObject
) extends Cursor { self =>
  def context: List[Either[Int, String]] = Right(key) :: parent.context

  def up: Cursor = {
    val newFocus = Json.fromJsonObject(if (changed) obj.add(key, focus) else obj)

    parent match {
      case _: CJson => CJson(newFocus)
      case a: CArray => a.copy(focus = newFocus, changed = self.changed || a.changed)
      case o: CObject => o.copy(focus = newFocus, changed = self.changed || o.changed)
    }
  }

  def delete: Cursor = {
    val newFocus = Json.fromJsonObject(obj.remove(key))

    parent match {
      case _: CJson => CJson(newFocus)
      case a: CArray => a.copy(focus = newFocus, changed = true)
      case o: CObject => o.copy(focus = newFocus, changed = true)
    }
  }

  def withFocus(f: Json => Json): Cursor = copy(focus = f(focus), changed = true)
  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[Cursor] =
    F.map(f(focus))(newFocus => copy(focus = newFocus, changed = true))

  def field(k: String): Cursor = obj(k) match {
    case Some(newFocus) => copy(focus = newFocus, key = k)
    case None => CFailure
  }

  def deleteGoField(k: String): Cursor = obj(k) match {
    case Some(newFocus) => copy(focus = newFocus, key = k, changed = true, obj = obj.remove(key))
    case None => CFailure
  }

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
}
