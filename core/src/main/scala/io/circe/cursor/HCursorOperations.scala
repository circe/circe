package io.circe.cursor

import io.circe._
import io.circe.CursorOpElement._

/**
 * A helper trait that implements cursor operations for [[io.circe.HCursor]].
 */
private[circe] trait HCursorOperations extends GenericCursor[HCursor] { this: HCursor =>
  type Focus[x] = x
  type Result = ACursor

  private[this] def toACursor(c: Option[Cursor], e: CursorOpElement) =
    c.fold(
      ACursor.fail(copy(history = CursorOp.fail(e) :: history))
    )(c => ACursor.ok(HCursor(c, CursorOp.ok(e) :: history)))

  def focus: Json = cursor.focus
  def top: Json = cursor.top
  def delete: ACursor = toACursor(cursor.delete, CursorOpDeleteGoParent)
  def withFocus(f: Json => Json): HCursor = HCursor(cursor.withFocus(f), history)


  def lefts: Option[List[Json]]     = cursor.lefts
  def rights: Option[List[Json]]    = cursor.rights
  def fieldSet: Option[Set[String]] = cursor.fieldSet
  def fields: Option[List[String]]  = cursor.fields

  def up: ACursor                          = toACursor(cursor.up, CursorOpUp)
  def left: ACursor                        = toACursor(cursor.left, CursorOpLeft)
  def right: ACursor                       = toACursor(cursor.right, CursorOpRight)
  def first: ACursor                       = toACursor(cursor.first, CursorOpFirst)
  def last: ACursor                        = toACursor(cursor.last, CursorOpLast)
  def leftN(n: Int): ACursor               = toACursor(cursor.leftN(n), CursorOpLeftN(n))
  def rightN(n: Int): ACursor              = toACursor(cursor.rightN(n), CursorOpRightN(n))
  def leftAt(p: Json => Boolean): ACursor  = toACursor(cursor.leftAt(p), CursorOpLeftAt(p))
  def rightAt(p: Json => Boolean): ACursor = toACursor(cursor.rightAt(p), CursorOpRightAt(p))
  def find(p: Json => Boolean): ACursor    = toACursor(cursor.find(p), CursorOpFind(p))
  def downArray: ACursor                   = toACursor(cursor.downArray, CursorOpDownArray)
  def downAt(p: Json => Boolean): ACursor  = toACursor(cursor.downAt(p), CursorOpDownAt(p))
  def downN(n: Int): ACursor               = toACursor(cursor.downN(n), CursorOpDownN(n))
  def field(k: String): ACursor            = toACursor(cursor.field(k), CursorOpField(k))
  def downField(k: String): ACursor        = toACursor(cursor.downField(k), CursorOpDownField(k))
  def deleteGoLeft: ACursor                = toACursor(cursor.deleteGoLeft, CursorOpDeleteGoLeft)
  def deleteGoRight: ACursor               = toACursor(cursor.deleteGoRight, CursorOpDeleteGoRight)
  def deleteGoFirst: ACursor               = toACursor(cursor.deleteGoFirst, CursorOpDeleteGoFirst)
  def deleteGoLast: ACursor                = toACursor(cursor.deleteGoLast, CursorOpDeleteGoLast)
  def deleteLefts: ACursor                 = toACursor(cursor.deleteLefts, CursorOpDeleteLefts)
  def deleteRights: ACursor                = toACursor(cursor.deleteRights, CursorOpDeleteRights)
  def setLefts(js: List[Json]): ACursor    = toACursor(cursor.setLefts(js), CursorOpSetLefts(js))
  def setRights(js: List[Json]): ACursor   = toACursor(cursor.setRights(js), CursorOpSetRights(js))
  def deleteGoField(k: String): ACursor
    = toACursor(cursor.deleteGoField(k), CursorOpDeleteGoField(k))

  def as[A](implicit d: Decoder[A]): Either[DecodingFailure, A] = d(this)
  def get[A](k: String)(implicit d: Decoder[A]): Either[DecodingFailure, A] = downField(k).as[A]
}
