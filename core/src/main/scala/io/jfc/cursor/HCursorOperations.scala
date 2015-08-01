package io.jfc.cursor

import cats.{ Id, Functor }
import cats.data.Xor
import io.jfc._

/**
 * A helper trait that implements cursor operations for [[io.jfc.HCursor]].
 */
trait HCursorOperations extends GenericCursor { this: HCursor =>
  type Self = HCursor
  type Focus[x] = Id[x]
  type Result = ACursor
  type M[x[_]] = Functor[x]

  def focus: Json = cursor.focus
  def undo: Json = cursor.undo
  def as[A](implicit decode: Decode[A]): Xor[DecodeFailure, A] = decode(this)
  def get[A](k: String)(implicit decode: Decode[A]): Xor[DecodeFailure, A] = downField(k).as[A]
  def withFocus(f: Json => Json): HCursor = HCursor(cursor.withFocus(f), history)
  def withFocusM[F[_]: Functor](f: Json => F[Json]): F[HCursor] =
    Functor[F].map(cursor.withFocusM(f))(c => HCursor(c, history))
  def lefts: Option[List[Json]] = cursor.lefts
  def rights: Option[List[Json]] = cursor.rights
  def fieldSet: Option[Set[String]] = cursor.fieldSet
  def fields: Option[List[String]] = cursor.fields
  def left: ACursor = history.acursorElement(cursor, _.left, CursorOpLeft)
  def right: ACursor = history.acursorElement(cursor, _.right, CursorOpRight)
  def first: ACursor = history.acursorElement(cursor, _.left, CursorOpFirst)
  def last: ACursor = history.acursorElement(cursor, _.left, CursorOpLast)
  def leftN(n: Int): ACursor = history.acursorElement(cursor, _.leftN(n), CursorOpLeftN(n))
  def rightN(n: Int): ACursor = history.acursorElement(cursor, _.rightN(n), CursorOpRightN(n))
  def leftAt(p: Json => Boolean): ACursor =
    history.acursorElement(cursor, _.leftAt(p), CursorOpLeftAt(p))
  def rightAt(p: Json => Boolean): ACursor =
    history.acursorElement(cursor, _.rightAt(p), CursorOpRightAt(p))
  def find(p: Json => Boolean): ACursor =
    history.acursorElement(cursor, _.find(p), CursorOpRightAt(p))
  def field(k: String): ACursor = history.acursorElement(cursor, _.field(k), CursorOpField(k))
  def downField(k: String): ACursor =
    history.acursorElement(cursor, _.downField(k), CursorOpDownField(k))
  def downArray: ACursor = history.acursorElement(cursor, _.downArray, CursorOpDownArray)
  def downAt(p: Json => Boolean): ACursor =
    history.acursorElement(cursor, _.downAt(p), CursorOpDownAt(p))
  def downN(n: Int): ACursor = history.acursorElement(cursor, _.downN(n), CursorOpDownN(n))
  def delete: ACursor = history.acursorElement(cursor, _.delete, CursorOpDeleteGoParent)
  def deleteGoLeft: ACursor = history.acursorElement(cursor, _.deleteGoLeft, CursorOpDeleteGoLeft)
  def deleteGoRight: ACursor =
    history.acursorElement(cursor, _.deleteGoRight, CursorOpDeleteGoRight)
  def deleteGoFirst: ACursor =
    history.acursorElement(cursor, _.deleteGoFirst, CursorOpDeleteGoFirst)
  def deleteGoLast: ACursor =
    history.acursorElement(cursor, _.deleteGoLast, CursorOpDeleteGoLast)
  def deleteGoField(k: String): ACursor =
    history.acursorElement(cursor, _.deleteGoField(k), CursorOpDeleteGoField(k))
  def deleteLefts: ACursor = history.acursorElement(cursor, _.deleteLefts, CursorOpDeleteLefts)
  def deleteRights: ACursor = history.acursorElement(cursor, _.deleteRights, CursorOpDeleteRights)
  def setLefts(js: List[Json]): ACursor =
    history.acursorElement(cursor, _.setLefts(js), CursorOpDeleteRights)
  def setRights(js: List[Json]): ACursor =
    history.acursorElement(cursor, _.setRights(js), CursorOpDeleteRights)
  def up: ACursor = history.acursorElement(cursor, _.up, CursorOpUp)
}
