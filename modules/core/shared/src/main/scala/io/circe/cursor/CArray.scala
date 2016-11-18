package io.circe.cursor

import cats.Applicative
import io.circe.{ Cursor, Json }

private[circe] final case class CArray(
  focus: Json,
  parent: Cursor,
  changed: Boolean,
  ls: List[Json],
  rs: List[Json]
) extends Cursor { self =>
  def context: List[Either[Int, String]] = Left(ls.length) :: parent.context

  def up: Cursor = {
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

  def delete: Cursor = {
    val newFocus = Json.fromValues(rs.reverse_:::(ls))

    parent match {
      case _: CJson => CJson(newFocus)
      case a: CArray => a.copy(focus = newFocus, changed = true)
      case o: CObject => o.copy(focus = newFocus, changed = true)
    }
  }

  def withFocus(f: Json => Json): Cursor = copy(focus = f(focus), changed = true)
  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[Cursor] =
    F.map(f(focus))(newFocus => copy(focus = newFocus, changed = true))

  def lefts: Option[List[Json]] = Some(ls)
  def rights: Option[List[Json]] = Some(rs)

  def left: Cursor = ls match {
    case h :: t => CArray(h, parent, changed, t, focus :: rs)
    case Nil => CFailure
  }

  def right: Cursor = rs match {
    case h :: t => CArray(h, parent, changed, focus :: ls, t)
    case Nil => CFailure
  }

  def first: Cursor = (focus :: rs).reverse_:::(ls) match {
    case h :: t => CArray(h, parent, changed, Nil, t)
    case Nil => CFailure
  }

  def last: Cursor = (focus :: ls).reverse_:::(rs) match {
    case h :: t => CArray(h, parent, changed, t, Nil)
    case Nil => CFailure
  }

  def deleteGoLeft: Cursor = ls match {
    case h :: t => CArray(h, parent, true, t, rs)
    case Nil => CFailure
  }

  def deleteGoRight: Cursor = rs match {
    case h :: t => CArray(h, parent, true, ls, t)
    case Nil => CFailure
  }

  def deleteGoFirst: Cursor = rs.reverse_:::(ls) match {
    case h :: t => CArray(h, parent, true, Nil, t)
    case Nil => CFailure
  }

  def deleteGoLast: Cursor = ls.reverse_:::(rs) match {
    case h :: t => CArray(h, parent, true, t, Nil)
    case Nil => CFailure
  }

  def deleteLefts: Cursor = copy(changed = true, ls = Nil)
  def deleteRights: Cursor = copy(changed = true, rs = Nil)
  def setLefts(js: List[Json]): Cursor = copy(changed = true, ls = js)
  def setRights(js: List[Json]): Cursor = copy(changed = true, rs = js)

  def field(k: String): Cursor = CFailure
  def deleteGoField(q: String): Cursor = CFailure
}
