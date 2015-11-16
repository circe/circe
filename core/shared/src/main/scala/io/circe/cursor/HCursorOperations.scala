package io.circe.cursor

import cats.{ Id, Functor }
import cats.data.Xor
import io.circe._

/**
 * A helper trait that implements cursor operations for [[io.circe.HCursor]].
 */
private[circe] trait HCursorOperations extends GenericCursor[HCursor] { this: HCursor =>
  type Focus[x] = Id[x]
  type Result = ACursor
  type M[x[_]] = Functor[x]

  private[this] def toACursor(c: Option[Cursor], e: CursorOp) =
    c.fold(
      ACursor.fail(copy(history = HistoryOp.fail(e) :: history))
    )(c => ACursor.ok(HCursor(c, HistoryOp.ok(e) :: history)))

  def focus: Json = cursor.focus
  def top: Json = cursor.top
  def delete: ACursor = toACursor(cursor.delete, CursorOp.DeleteGoParent)
  def withFocus(f: Json => Json): HCursor = HCursor(cursor.withFocus(f), history)
  def withFocusM[F[_]: Functor](f: Json => F[Json]): F[HCursor] =
    Functor[F].map(cursor.withFocusM(f))(c => HCursor(c, history))

  def lefts: Option[List[Json]]     = cursor.lefts
  def rights: Option[List[Json]]    = cursor.rights
  def fieldSet: Option[Set[String]] = cursor.fieldSet
  def fields: Option[List[String]]  = cursor.fields

  def up: ACursor                          = toACursor(cursor.up, CursorOp.MoveUp)
  def left: ACursor                        = toACursor(cursor.left, CursorOp.MoveLeft)
  def right: ACursor                       = toACursor(cursor.right, CursorOp.MoveRight)
  def first: ACursor                       = toACursor(cursor.first, CursorOp.MoveFirst)
  def last: ACursor                        = toACursor(cursor.last, CursorOp.MoveLast)
  def leftN(n: Int): ACursor               = toACursor(cursor.leftN(n), CursorOp.LeftN(n))
  def rightN(n: Int): ACursor              = toACursor(cursor.rightN(n), CursorOp.RightN(n))
  def leftAt(p: Json => Boolean): ACursor  = toACursor(cursor.leftAt(p), CursorOp.LeftAt(p))
  def rightAt(p: Json => Boolean): ACursor = toACursor(cursor.rightAt(p), CursorOp.RightAt(p))
  def find(p: Json => Boolean): ACursor    = toACursor(cursor.find(p), CursorOp.Find(p))
  def downArray: ACursor                   = toACursor(cursor.downArray, CursorOp.DownArray)
  def downAt(p: Json => Boolean): ACursor  = toACursor(cursor.downAt(p), CursorOp.DownAt(p))
  def downN(n: Int): ACursor               = toACursor(cursor.downN(n), CursorOp.DownN(n))
  def field(k: String): ACursor            = toACursor(cursor.field(k), CursorOp.Field(k))
  def downField(k: String): ACursor        = toACursor(cursor.downField(k), CursorOp.DownField(k))
  def deleteGoLeft: ACursor                = toACursor(cursor.deleteGoLeft, CursorOp.DeleteGoLeft)
  def deleteGoRight: ACursor               = toACursor(cursor.deleteGoRight, CursorOp.DeleteGoRight)
  def deleteGoFirst: ACursor               = toACursor(cursor.deleteGoFirst, CursorOp.DeleteGoFirst)
  def deleteGoLast: ACursor                = toACursor(cursor.deleteGoLast, CursorOp.DeleteGoLast)
  def deleteLefts: ACursor                 = toACursor(cursor.deleteLefts, CursorOp.DeleteLefts)
  def deleteRights: ACursor                = toACursor(cursor.deleteRights, CursorOp.DeleteRights)
  def setLefts(js: List[Json]): ACursor    = toACursor(cursor.setLefts(js), CursorOp.SetLefts(js))
  def setRights(js: List[Json]): ACursor   = toACursor(cursor.setRights(js), CursorOp.SetRights(js))
  def deleteGoField(k: String): ACursor
    = toACursor(cursor.deleteGoField(k), CursorOp.DeleteGoField(k))

  def as[A](implicit d: Decoder[A]): Decoder.Result[A] = d(this)
  def get[A](k: String)(implicit d: Decoder[A]): Decoder.Result[A] = downField(k).as[A]
}
