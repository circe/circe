package io.circe.cursor

import io.circe.{ Context, Cursor, Json }

private[circe] case class CArray(
  focus: Json,
  p: Cursor,
  u: Boolean,
  ls: List[Json],
  rs: List[Json]
) extends Cursor {
  def context: List[Context] = Context.inArray(focus, ls.length) :: p.context

  def up: Option[Cursor] = Some {
    val j = Json.fromValues((focus :: rs).reverse_:::(ls))

    p match {
      case CJson(_) => CJson(j)
      case CArray(_, pp, v, pls, prs) => CArray(j, pp, u || v, pls, prs)
      case CObject(_, pk, pp, v, po) => CObject(j, pk, pp, u || v, if (u) po + (pk, j) else po)
    }
  }

  def delete: Option[Cursor] = Some {
    val j = Json.fromValues(rs.reverse_:::(ls))

    p match {
      case CJson(_) => CJson(j)
      case CArray(_, pp, _, pls, prs) => CArray(j, pp, true, pls, prs)
      case CObject(_, pk, pp, _, po) => CObject(j, pk, pp, true, po)
    }
  }

  def withFocus(f: Json => Json): Cursor = copy(focus = f(focus), u = true)

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

  override def first: Option[Cursor] = (focus :: rs).reverse_:::(ls) match {
    case h :: t => Some(CArray(h, p, u, Nil, t))
    case Nil => None
  }

  override def last: Option[Cursor] = (focus :: ls).reverse_:::(rs) match {
    case h :: t => Some(CArray(h, p, u, t, Nil))
    case Nil => None
  }

  override def deleteGoLeft: Option[Cursor] = ls match {
    case h :: t => Some(CArray(h, p, true, t, rs))
    case Nil => None
  }

  override def deleteGoRight: Option[Cursor] = rs match {
    case h :: t => Some(CArray(h, p, true, ls, t))
    case Nil => None
  }

  override def deleteGoFirst: Option[Cursor] = rs.reverse_:::(ls) match {
    case h :: t => Some(CArray(h, p, true, Nil, t))
    case Nil => None
  }

  override def deleteGoLast: Option[Cursor] = ls.reverse_:::(rs) match {
    case h :: t => Some(CArray(h, p, true, t, Nil))
    case Nil => None
  }

  override def deleteLefts: Option[Cursor] = Some(copy(u = true, ls = Nil))
  override def deleteRights: Option[Cursor] = Some(copy(u = true, rs = Nil))
  override def setLefts(js: List[Json]): Option[Cursor] = Some(copy(u = true, ls = js))
  override def setRights(js: List[Json]): Option[Cursor] = Some(copy(u = true, rs = js))
}
