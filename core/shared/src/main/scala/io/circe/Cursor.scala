package io.circe

import algebra.Eq
import cats.{ Functor, Id, Show }
import cats.std.list._
import io.circe.cursor.{ CArray, CJson, CObject }
import scala.annotation.tailrec

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
abstract class Cursor extends GenericCursor[Cursor] {
  type Focus[x] = Id[x]
  type Result = Option[Cursor]
  type M[x[_]] = Functor[x]
  /**
   * Return the current context of the focus.
   */
  def context: List[Context]

  final def top: Json = {
    @tailrec
    def go(c: Cursor): Json = c match {
      case CJson(j) => j
      case CArray(j, p, changed, ls, rs) =>
        val newFocus = Json.fromValues((j :: rs).reverse_:::(ls))

        go(
          p match {
            case _: CJson => CJson(newFocus)
            case a: CArray => a.copy(focus = newFocus, changed = changed || a.changed)
            case o: CObject => o.copy(
              focus = newFocus,
              changed = changed || o.changed,
              obj = if (changed) o.obj.add(o.key, newFocus) else o.obj
            )
          }
        )
      case CObject(j, k, p, changed, obj) =>
        val newFocus = Json.fromJsonObject(if (changed) obj.add(k, j) else obj)

        go(
          p match {
            case _: CJson => CJson(newFocus)
            case a: CArray => a.copy(focus = newFocus, changed = changed || a.changed)
            case o: CObject => o.copy(focus = newFocus, changed = changed || o.changed)
          }
        )
    }

    go(this)
  }

  final def fieldSet: Option[Set[String]] = focus.asObject.map(_.fieldSet)
  final def fields: Option[List[String]] = focus.asObject.map(_.fields)

  final def leftN(n: Int): Option[Cursor] = if (n < 0) rightN(-n) else {
    @tailrec
    def go(i: Int, c: Option[Cursor]): Option[Cursor] = if (i == 0) c else {
      go(i - 1, c.flatMap(_.left))
    }
    go(n, Some(this))
  }

  final def rightN(n: Int): Option[Cursor] = if (n < 0) leftN(-n) else {
    @tailrec
    def go(i: Int, c: Option[Cursor]): Option[Cursor] = if (i == 0) c else {
      go(i - 1, c.flatMap(_.right))
    }
    go(n, Some(this))
  }

  def leftAt(p: Json => Boolean): Option[Cursor] = {
    @tailrec
    def go(c: Option[Cursor]): Option[Cursor] = c match {
      case None => None
      case Some(z) => if (p(z.focus)) Some(z) else go(z.left)
    }

    go(left)
  }

  final def rightAt(p: Json => Boolean): Option[Cursor] = right.flatMap(_.find(p))

  final def find(p: Json => Boolean): Option[Cursor] = {
    @annotation.tailrec
    def go(c: Option[Cursor]): Option[Cursor] = c match {
      case None => None
      case Some(z) => if (p(z.focus)) Some(z) else go(z.right)
    }

    go(Some(this))
  }

  final def downArray: Option[Cursor] = focus.asArray match {
    case Some(h :: t) => Some(CArray(h, this, false, Nil, t))
    case Some(Nil) => None
    case None => None
  }

  final def downAt(p: Json => Boolean): Option[Cursor] = downArray.flatMap(_.find(p))

  final def downN(n: Int): Option[Cursor] = downArray.flatMap(_.rightN(n))

  final def downField(k: String): Option[Cursor] = focus.asObject match {
    case Some(o) => o(k) match {
      case Some(j) => Some(CObject(j, k, this, false, o))
      case None => None
    }
    case None => None
  }

  final def as[A](implicit d: Decoder[A]): Decoder.Result[A] = HCursor.fromCursor(this).as[A]
  final def get[A](k: String)(implicit d: Decoder[A]): Decoder.Result[A] = HCursor.fromCursor(this).get[A](k)

  final def replay(history: List[HistoryOp]): Option[Cursor] =
    ACursor.ok(HCursor.fromCursor(this)).replay(history).success.map(_.cursor)
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
