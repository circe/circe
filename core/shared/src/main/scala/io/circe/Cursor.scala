package io.circe

import algebra.{ Eq, Monoid }
import cats.Show
import cats.std.list._
import io.circe.cursor.{ CArray, CJson, CObject, CursorOperations }

/**
 * A zipper that represents a position in a JSON value and supports navigation around the JSON
 * value.
 *
 * The `focus` represents the current position of the cursor; it may be updated with `withFocus` or
 * changed using the navigation methods `left`, `right`, etc.
 *
 * @groupname Ungrouped Cursor fields and operations
 * @groupprio Ungrouped 1
 *
 * @see [[GenericCursor]]
 * @author Travis Brown
 */
abstract class Cursor extends CursorOperations {
  /**
   * Return the current context of the focus.
   */
  def context: List[Context]

  /**
   * Create an [[HCursor]] for this cursor in order to track history.
   */
  final def hcursor: HCursor = new HCursor(this) {
    final def history: List[HistoryOp] = Nil
  }
}

final object Cursor {
  /**
   * Create a new cursor with no context.
   */
  def apply(j: Json): Cursor = CJson(j)

  implicit final val showCursor: Show[Cursor] = Show.show { c =>
    val sc = Show[Context]
    s"${ c.context.map(e => sc.show(e)).mkString(", ") } ==> ${ Show[Json].show(c.focus) }"
  }

  implicit final val eqCursor: Eq[Cursor] = Eq.instance {
    case (CJson(j1), CJson(j2)) => Eq[Json].eqv(j1, j2)
    case (CArray(f1, p1, _, l1, r1), CArray(f2, p2, _, l2, r2)) =>
      eqCursor.eqv(p1, p2) && Eq[List[Json]].eqv(l1, l2) &&
        Eq[Json].eqv(f1, f2) && Eq[List[Json]].eqv(r1, r2)
    case (CObject(f1, k1, p1, _, o1), CObject(f2, k2, p2, _, o2)) =>
      eqCursor.eqv(p1, p2) && Eq[JsonObject].eqv(o1, o2) && k1 == k2 && Eq[Json].eqv(f1, f2)
    case (_, _) => false
  }
}
