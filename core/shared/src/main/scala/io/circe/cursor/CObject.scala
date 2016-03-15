package io.circe.cursor

import cats.Functor
import io.circe.{ Context, Cursor, Json, JsonObject }

private[circe] final case class CObject(
  focus: Json,
  key: String,
  parent: Cursor,
  changed: Boolean,
  obj: JsonObject
) extends Cursor { self =>
  def context: List[Context] = Context.inObject(focus, key) :: parent.context

  def up: Option[Cursor] = Some {
    val newFocus = Json.fromJsonObject(if (changed) obj.add(key, focus) else obj)

    parent match {
      case _: CJson => CJson(newFocus)
      case a: CArray => a.copy(focus = newFocus, changed = self.changed || a.changed)
      case o: CObject => o.copy(focus = newFocus, changed = self.changed || o.changed)
    }
  }

  def delete: Option[Cursor] = Some {
    val newFocus = Json.fromJsonObject(obj.remove(key))

    parent match {
      case _: CJson => CJson(newFocus)
      case a: CArray => a.copy(focus = newFocus, changed = true)
      case o: CObject => o.copy(focus = newFocus, changed = true)
    }
  }

  def withFocus(f: Json => Json): Cursor = copy(focus = f(focus), changed = true)
  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Functor[F]): F[Cursor] =
    F.map(f(focus))(newFocus => copy(focus = newFocus, changed = true))

  def field(k: String): Option[Cursor] = obj(k).map { newFocus =>
    copy(focus = newFocus, key = k)
  }

  def deleteGoField(k: String): Option[Cursor] = obj(k).map { newFocus =>
    copy(focus = newFocus, key = k, changed = true, obj = obj.remove(key))
  }

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
}
