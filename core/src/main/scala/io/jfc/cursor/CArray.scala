package io.jfc.cursor

import cats.Functor
import io.jfc.{ Context, ContextElement, Cursor, Json }

private[jfc] case class CArray(
  focus: Json,
  p: Cursor,
  u: Boolean,
  ls: List[Json],
  rs: List[Json]
) extends Cursor {
  def context: Context = ContextElement.arrayContext(focus, ls.length) +: p.context
  def withFocus(f: Json => Json): Cursor = copy(focus = f(focus), u = true)
  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Functor[F]): F[Cursor] =
    F.map(f(focus))(j => copy(focus = j, u = true))

  override def lefts: Option[List[Json]] = Some(ls)
  override def rights: Option[List[Json]] = Some(rs)

  override def left: Option[Cursor] = ls match {
    case h :: t => Some(CArray(h, p, u, t, focus :: rs))
    case Nil => None
  }

  override def right: Option[Cursor] = rs match {
    case h :: t => Some(CArray(h, p, u, focus :: ls, t))
    case Nil => None
  }

  override def first: Option[Cursor] = ls.reverse_:::(focus :: rs) match {
    case h :: t => Some(CArray(h, p, u, Nil, t))
    case Nil => None
  }

  override def last: Option[Cursor] = rs.reverse_:::(focus :: ls) match {
    case h :: t => Some(CArray(h, p, u, t, Nil))
    case Nil => None
  }

  def delete: Option[Cursor] = Some {
    val j = Json.fromValues(ls.reverse_:::(rs))

    p match {
      case CJson(_) => CJson(j)
      case CArray(_, pp, _, pls, prs) => CArray(j, pp, true, pls, prs)
      case CObject(_, pk, pp, _, po) => CObject(j, pk, pp, true, po)
    }
  }

  override def deleteGoLeft: Option[Cursor] = ls match {
    case h :: t => Some(CArray(h, p, true, t, rs))
    case Nil => None
  }

  override def deleteGoRight: Option[Cursor] = rs match {
    case h :: t => Some(CArray(h, p, true, ls, t))
    case Nil => None
  }

  override def deleteGoFirst: Option[Cursor] = ls.reverse_:::(rs) match {
    case h :: t => Some(CArray(h, p, true, Nil, t))
    case Nil => None
  }

  override def deleteGoLast: Option[Cursor] = rs.reverse_:::(ls) match {
    case h :: t => Some(CArray(h, p, true, t, Nil))
    case Nil => None
  }

  override def deleteLefts: Option[Cursor] = Some(copy(u = true, ls = Nil))
  override def deleteRights: Option[Cursor] = Some(copy(u = true, rs = Nil))

  override def setLefts(js: List[Json]): Option[Cursor] = Some(copy(u = true, ls = js))
  override def setRights(js: List[Json]): Option[Cursor] = Some(copy(u = true, rs = js))

  def up: Option[Cursor] = Some {
    val j = Json.fromValues(ls.reverse_:::(focus :: rs))

    p match {
      case CJson(_) => CJson(j)
      case CArray(_, pp, v, pls, prs) => CArray(j, pp, u || v, pls, prs)
      case CObject(_, pk, pp, v, po) => CObject(j, pk, pp, u || v, if (u) po + (pk, j) else po)
    }
  }
}
