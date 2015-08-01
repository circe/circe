package io.jfc

import algebra.{ Eq, Monoid }
import cats.Show
import cats.functor.Contravariant
import cats.std.list._

/**
 * A list of elements denoting the history of a cursor.
 *
 * Note: Most recent operation appears at head of list.
 *
 * @author Tony Morris
 */
case class CursorHistory(toList: List[CursorOp]) {
  def head: Option[CursorOp] = toList.headOption

  /**
   * Append two lists of cursor history.
   */
  def ++(h: CursorHistory): CursorHistory = CursorHistory(toList ++ h.toList)

  /**
   * Prepend a cursor operation to the history.
   */
  def +:(o: CursorOp): CursorHistory = CursorHistory(o +: toList)

  def failedACursor(c: Cursor): ACursor = ACursor.fail(HCursor(c, this))

  def acursor(c: Cursor): ACursor = ACursor.ok(HCursor(c, this))

  def acursorElement(c: Cursor, f: Cursor => Option[Cursor], e: CursorOpElement): ACursor = {
    f(c).fold(
      +:(CursorOp.failedOp(e)).failedACursor(c)
    ) { q => +:(CursorOp(e)).acursor(q) }
  }

  override def toString(): String =
    "CursorHistory(%s)".format(CursorHistory.showCursorHistory.show(this))
}

object CursorHistory {
  def start(e: CursorOp) = CursorHistory(List(e))

  implicit val eqCursorHistory: Eq[CursorHistory] = Eq.by(_.toList)
  implicit val showCursorHistory: Show[CursorHistory] = {
    val opShow = Show[CursorOp]
    Show.show { h =>
      val items = h.toList.map(opShow.show).mkString(",")
      s"[$items]"
    }
  }
  implicit val monoidCursorHistory: Monoid[CursorHistory] = new Monoid[CursorHistory] {
    val empty: CursorHistory = CursorHistory(Nil)
    def combine(x: CursorHistory, y: CursorHistory): CursorHistory =
      CursorHistory(x.toList ::: y.toList)
  }
}
