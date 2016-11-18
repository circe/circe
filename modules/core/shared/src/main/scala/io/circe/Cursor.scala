package io.circe

import cats.{ Applicative, Eq, Id, Show }
import cats.instances.list._
import io.circe.cursor.{ CArray, CFailure, CJson, CObject }
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
  type Result = Cursor
  type M[x[_]] = Applicative[x]
  /**
   * Return the current context of the focus.
   */
  def context: List[Either[Int, String]]

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

  final def leftN(n: Int): Cursor = if (n < 0) rightN(-n) else {
    @tailrec
    def go(i: Int, c: Cursor): Cursor = if (i == 0) c else {
      go(i - 1, c.left)
    }
    go(n, this)
  }

  final def rightN(n: Int): Cursor = if (n < 0) leftN(-n) else {
    @tailrec
    def go(i: Int, c: Cursor): Cursor = if (i == 0) c else {
      go(i - 1, c.right)
    }
    go(n, this)
  }

  def leftAt(p: Json => Boolean): Cursor = {
    @tailrec
    def go(c: Cursor): Cursor = c match {
      case CFailure => CFailure
      case other => if (p(other.focus)) other else go(other.left)
    }

    go(left)
  }

  final def rightAt(p: Json => Boolean): Cursor = right.find(p)

  final def find(p: Json => Boolean): Cursor = {
    @annotation.tailrec
    def go(c: Cursor): Cursor = c match {
      case CFailure => CFailure
      case other => if (p(other.focus)) other else go(other.right)
    }

    go(this)
  }

  final def downArray: Cursor = focus match {
    case Json.JArray(h :: t) => CArray(h, this, false, Nil, t)
    case _ => CFailure
  }

  final def downAt(p: Json => Boolean): Cursor = downArray.find(p)

  final def downN(n: Int): Cursor = downArray.rightN(n)

  final def downField(k: String): Cursor = focus match {
    case Json.JObject(o) =>
      val m = o.toMap

      if (m.contains(k)) CObject(m(k), k, this, false, o) else CFailure
    case _ => CFailure
  }

  final def as[A](implicit d: Decoder[A]): Decoder.Result[A] = HCursor.fromCursor(this).as[A]
  final def get[A](k: String)(implicit d: Decoder[A]): Decoder.Result[A] = HCursor.fromCursor(this).get[A](k)

  final def replay(history: List[HistoryOp]): Cursor =
    ACursor.ok(HCursor.fromCursor(this)).replay(history).success.fold[Cursor](CFailure)(_.cursor)
}

final object Cursor {
  /**
   * Create a new cursor with no context.
   */
  def apply(j: Json): Cursor = CJson(j)

  private[this] val printContext: Either[Int, String] => String = _ match {
    case Right(k) => s"{$k}"
    case Left(i) => s"[$i]"
  }

  implicit final val showCursor: Show[Cursor] = Show.show { c =>
    s"${ c.context.map(printContext).mkString(", ") } ==> ${ Show[Json].show(c.focus) }"
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
