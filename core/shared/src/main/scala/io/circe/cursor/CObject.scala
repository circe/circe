package io.circe.cursor

import cats.Functor
import io.circe.{ Context, Cursor, Json, JsonObject }

private[circe] case class CObject(
  focus: Json,
  key: String,
  parent: Cursor,
  changed: Boolean,
  obj: JsonObject
) extends Cursor { self =>
  def context: List[Context] = Context.inObject(focus, key) :: parent.context

  def up: Option[Cursor] = Some {
    val newFocus = Json.fromJsonObject(if (changed) obj.add(key, focus) else obj)

    parent.normalize match {
      case _: CJson => CJson(newFocus)
      case a: CArray => a.copy(focus = newFocus, changed = self.changed || a.changed)
      case o: CObject => o.copy(focus = newFocus, changed = self.changed || o.changed)
    }
  }

  def delete: Option[Cursor] = Some {
    val newFocus = Json.fromJsonObject(obj.remove(key))

    parent.normalize match {
      case _: CJson => CJson(newFocus)
      case a: CArray => a.copy(focus = newFocus, changed = true)
      case o: CObject => o.copy(focus = newFocus, changed = true)
    }
  }

  def withFocus(f: Json => Json): Cursor = copy(focus = f(focus), changed = true)
  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Functor[F]): F[Cursor] =
    F.map(f(focus))(newFocus => copy(focus = newFocus, changed = true))

  override def field(k: String): Option[Cursor] = obj(k).map { newFocus =>
    copy(focus = newFocus, key = k)
  }

  override def deleteGoField(k: String): Option[Cursor] = obj(k).map { newFocus =>
    copy(focus = newFocus, key = k, changed = true, obj = obj.remove(key))
  }
}
