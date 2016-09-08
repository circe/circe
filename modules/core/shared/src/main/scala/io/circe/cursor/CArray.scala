package io.circe.cursor

import cats.Functor
import io.circe.{ Context, Cursor, Json }

private[circe] final case class CArray(
  focus: Json,
  parent: Cursor,
  changed: Boolean,
  ls: List[Json],
  rs: List[Json]
) extends Cursor { self =>
  def context: List[Context] = Context.inArray(focus, ls.length) :: parent.context

  def up: Option[Cursor] = Some {
    val newFocus = Json.fromValues((focus :: rs).reverse_:::(ls))

    parent match {
      case _: CJson => CJson(newFocus)
      case a: CArray => a.copy(focus = newFocus, changed = self.changed || a.changed)
      case o: CObject => o.copy(
        focus = newFocus,
        changed = self.changed || o.changed,
        obj = if (self.changed) o.obj.add(o.key, newFocus) else o.obj
      )
    }
  }

  def delete: Option[Cursor] = Some {
    val newFocus = Json.fromValues(rs.reverse_:::(ls))

    parent match {
      case _: CJson => CJson(newFocus)
      case a: CArray => a.copy(focus = newFocus, changed = true)
      case o: CObject => o.copy(focus = newFocus, changed = true)
    }
  }

  def withFocus(f: Json => Json): Cursor = copy(focus = f(focus), changed = true)
  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Functor[F]): F[Cursor] =
    F.map(f(focus))(newFocus => copy(focus = newFocus, changed = true))

  def lefts: Option[List[Json]] = Some(ls)
  def rights: Option[List[Json]] = Some(rs)

  def left: Option[Cursor] = ls match {
    case h :: t => Some(CArray(h, parent, changed, t, focus :: rs))
    case Nil => None
  }

  def right: Option[Cursor] = rs match {
    case h :: t => Some(CArray(h, parent, changed, focus :: ls, t))
    case Nil => None
  }

  def first: Option[Cursor] = (focus :: rs).reverse_:::(ls) match {
    case h :: t => Some(CArray(h, parent, changed, Nil, t))
    case Nil => None
  }

  def last: Option[Cursor] = (focus :: ls).reverse_:::(rs) match {
    case h :: t => Some(CArray(h, parent, changed, t, Nil))
    case Nil => None
  }

  def deleteGoLeft: Option[Cursor] = ls match {
    case h :: t => Some(CArray(h, parent, true, t, rs))
    case Nil => None
  }

  def deleteGoRight: Option[Cursor] = rs match {
    case h :: t => Some(CArray(h, parent, true, ls, t))
    case Nil => None
  }

  def deleteGoFirst: Option[Cursor] = rs.reverse_:::(ls) match {
    case h :: t => Some(CArray(h, parent, true, Nil, t))
    case Nil => None
  }

  def deleteGoLast: Option[Cursor] = ls.reverse_:::(rs) match {
    case h :: t => Some(CArray(h, parent, true, t, Nil))
    case Nil => None
  }

  def deleteLefts: Option[Cursor] = Some(copy(changed = true, ls = Nil))
  def deleteRights: Option[Cursor] = Some(copy(changed = true, rs = Nil))
  def setLefts(js: List[Json]): Option[Cursor] = Some(copy(changed = true, ls = js))
  def setRights(js: List[Json]): Option[Cursor] = Some(copy(changed = true, rs = js))

  def field(k: String): Option[Cursor] = None
  def deleteGoField(q: String): Option[Cursor] = None
}
