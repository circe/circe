package io.circe.cursor

import cats.Functor
import io.circe.{ Context, Cursor, Json, JsonObject }

private[circe] case class CObject(
  focus: Json,
  key: String,
  p: Cursor,
  u: Boolean,
  o: JsonObject
) extends Cursor {
  def context: List[Context] = Context.inObject(focus, key) :: p.context

  def up: Option[Cursor] = Some {
    val j = Json.fromJsonObject(if (u) o + (key, focus) else o)

    p match {
      case CJson(_) => CJson(j)
      case CArray(_, pp, v, pls, prs) => CArray(j, pp, u || v, pls, prs)
      case CObject(_, pk, pp, v, po) => CObject(j, pk, pp, u || v, po)
    }
  }

  def delete: Option[Cursor] = Some {
    val j = Json.fromJsonObject(o - key)

    p match {
      case CJson(_) => CJson(j)
      case CArray(_, pp, _, pls, prs) => CArray(j, pp, true, pls, prs)
      case CObject(_, pk, pp, _, po) => CObject(j, pk, pp, true, po)
    }
  }

  def withFocus(f: Json => Json): Cursor = copy(focus = f(focus), u = true)
  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Functor[F]): F[Cursor] =
    F.map(f(focus))(j => copy(focus = j, u = true))

  override def field(k: String): Option[Cursor] = o(k).map { j =>
    copy(focus = j, key = k)
  }

  override def deleteGoField(k: String): Option[Cursor] = o(k).map { j =>
    copy(focus = j, key = k, u = true, o = o - key)
  }
}
