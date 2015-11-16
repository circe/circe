package io.circe.cursor

import cats.{ Id, Functor }
import cats.data.Xor
import io.circe.{ Cursor, Decoder, DecodingFailure, GenericCursor, Json }
import scala.annotation.tailrec

/**
 * A helper trait that implements cursor operations for [[io.circe.Cursor]].
 */
private[circe] trait CursorOperations extends GenericCursor[Cursor] { this: Cursor =>
  type Focus[x] = Id[x]
  type Result = Option[Cursor]
  type M[x[_]] = Functor[x]

  def top: Json = {
    @tailrec
    def go(c: Cursor): Json = c.normalize match {
      case CJson(j) => j
      case CArray(j, p, changed, ls, rs) =>
        val newFocus = Json.fromValues((j :: rs).reverse_:::(ls))

        go(
          p.normalize match {
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
          p.normalize match {
            case _: CJson => CJson(newFocus)
            case a: CArray => a.copy(focus = newFocus, changed = changed || a.changed)
            case o: CObject => o.copy(focus = newFocus, changed = changed || o.changed)
          }
        )
    }

    go(this)
  }

  def lefts: Option[List[Json]] = None
  def rights: Option[List[Json]] = None

  def fieldSet: Option[Set[String]] = focus.asObject.map(_.fieldSet)
  def fields: Option[List[String]] = focus.asObject.map(_.fields)

  def left: Option[Cursor] = None
  def right: Option[Cursor] = None
  def first: Option[Cursor] = None
  def last: Option[Cursor] = None

  def leftN(n: Int): Option[Cursor] = if (n < 0) rightN(-n) else {
    @tailrec
    def go(i: Int, c: Option[Cursor]): Option[Cursor] = if (i == 0) c else {
      go(i - 1, c.flatMap(_.left))
    }
    go(n, Some(this))
  }

  def rightN(n: Int): Option[Cursor] = if (n < 0) leftN(-n) else {
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
      case Some(z) => go(if (p(z.focus)) Some(z) else z.left)
    }

    go(left)
  }

  def rightAt(p: Json => Boolean): Option[Cursor] = right.flatMap(_.find(p))

  def find(p: Json => Boolean): Option[Cursor] = {
    @annotation.tailrec
    def go(c: Option[Cursor]): Option[Cursor] = c match {
      case None => None
      case Some(z) => if (p(z.focus)) Some(z) else go(z.right)
    }

    go(Some(this))
  }

  def downArray: Option[Cursor] = focus.asArray.flatMap { values =>
    if (values.isEmpty) None else Some(CFastNavArray(values.toIndexedSeq, 0, this))
  }

  def downAt(p: Json => Boolean): Option[Cursor] =
    downArray.flatMap(_.find(p))

  def downN(n: Int): Option[Cursor] = downArray.flatMap(_.rightN(n))

  def field(k: String): Option[Cursor] = None

  def downField(k: String): Option[Cursor] =
    focus.asObject.flatMap(o => o(k).map(j => CObject(j, k, this, false, o)))

  def deleteGoLeft: Option[Cursor] = None
  def deleteGoRight: Option[Cursor] = None
  def deleteGoFirst: Option[Cursor] = None
  def deleteGoLast: Option[Cursor] = None
  def deleteLefts: Option[Cursor] = None
  def deleteRights: Option[Cursor] = None
  def setLefts(x: List[Json]): Option[Cursor] = None
  def setRights(x: List[Json]): Option[Cursor] = None

  def deleteGoField(q: String): Option[Cursor] = None

  def as[A](implicit d: Decoder[A]): Decoder.Result[A] = hcursor.as[A]
  def get[A](k: String)(implicit d: Decoder[A]): Decoder.Result[A] = hcursor.get[A](k)
}
