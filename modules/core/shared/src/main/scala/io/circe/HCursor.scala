package io.circe

import cats.{ Eq, Functor, Id }
import scala.collection.immutable.Set

/**
 * A cursor that tracks the history of operations performed with it.
 *
 * @groupname Ungrouped HCursor fields and operations
 * @groupprio Ungrouped 1
 *
 * @see [[GenericCursor]]
 * @author Travis Brown
 */
sealed abstract class HCursor(final val cursor: Cursor) extends GenericCursor[HCursor] { self =>
  type Focus[x] = Id[x]
  type Result = ACursor
  type M[x[_]] = Functor[x]

  def history: List[HistoryOp]

  /**
   * Create an [[ACursor]] for this cursor.
   */
  final def acursor: ACursor = ACursor.ok(this)

  /**
   * Create a failed [[ACursor]] for this cursor.
   */
  final def failedACursor: ACursor = ACursor.fail(this)

  /**
   * If the last operation was not successful, reattempt it.
   */
  final def reattempted: HCursor = new HCursor(self.cursor) {
    final def history: List[HistoryOp] = HistoryOp.reattempt :: self.history
  }

  @inline private[this] def toACursor(oc: Option[Cursor], e: CursorOp) = oc match {
    case None => ACursor.fail(
      new HCursor(this.cursor) {
        private[this] val incorrectFocus: Boolean =
          (e.requiresObject && !self.focus.isObject) || (e.requiresArray && !self.focus.isArray)
        final def history: List[HistoryOp] = HistoryOp.fail(e, incorrectFocus) :: self.history
      }
    )
    case Some(c) => ACursor.ok(
      new HCursor(c) {
        final def history: List[HistoryOp] = HistoryOp.ok(e) :: self.history
      }
    )
  }

  final def focus: Json = cursor.focus
  final def top: Json = cursor.top
  final def delete: ACursor = toACursor(cursor.delete, CursorOp.DeleteGoParent)
  final def withFocus(f: Json => Json): HCursor = new HCursor(cursor.withFocus(f)) {
    final def history: List[HistoryOp] = self.history
  }

  final def withFocusM[F[_]: Functor](f: Json => F[Json]): F[HCursor] =
    Functor[F].map(cursor.withFocusM(f))(c =>
      new HCursor(c) {
        final def history: List[HistoryOp] = self.history
      }
    )


  final def lefts: Option[List[Json]]     = cursor.lefts
  final def rights: Option[List[Json]]    = cursor.rights
  final def fieldSet: Option[Set[String]] = cursor.fieldSet
  final def fields: Option[List[String]]  = cursor.fields

  final def up: ACursor                          = toACursor(cursor.up, CursorOp.MoveUp)
  final def left: ACursor                        = toACursor(cursor.left, CursorOp.MoveLeft)
  final def right: ACursor                       = toACursor(cursor.right, CursorOp.MoveRight)
  final def first: ACursor                       = toACursor(cursor.first, CursorOp.MoveFirst)
  final def last: ACursor                        = toACursor(cursor.last, CursorOp.MoveLast)
  final def leftN(n: Int): ACursor               = toACursor(cursor.leftN(n), CursorOp.LeftN(n))
  final def rightN(n: Int): ACursor              = toACursor(cursor.rightN(n), CursorOp.RightN(n))
  final def leftAt(p: Json => Boolean): ACursor  = toACursor(cursor.leftAt(p), CursorOp.LeftAt(p))
  final def rightAt(p: Json => Boolean): ACursor = toACursor(cursor.rightAt(p), CursorOp.RightAt(p))
  final def find(p: Json => Boolean): ACursor    = toACursor(cursor.find(p), CursorOp.Find(p))
  final def downArray: ACursor                   = toACursor(cursor.downArray, CursorOp.DownArray)
  final def downAt(p: Json => Boolean): ACursor  = toACursor(cursor.downAt(p), CursorOp.DownAt(p))
  final def downN(n: Int): ACursor               = toACursor(cursor.downN(n), CursorOp.DownN(n))
  final def field(k: String): ACursor            = toACursor(cursor.field(k), CursorOp.Field(k))
  final def downField(k: String): ACursor        = toACursor(cursor.downField(k), CursorOp.DownField(k))
  final def deleteGoLeft: ACursor                = toACursor(cursor.deleteGoLeft, CursorOp.DeleteGoLeft)
  final def deleteGoRight: ACursor               = toACursor(cursor.deleteGoRight, CursorOp.DeleteGoRight)
  final def deleteGoFirst: ACursor               = toACursor(cursor.deleteGoFirst, CursorOp.DeleteGoFirst)
  final def deleteGoLast: ACursor                = toACursor(cursor.deleteGoLast, CursorOp.DeleteGoLast)
  final def deleteLefts: ACursor                 = toACursor(cursor.deleteLefts, CursorOp.DeleteLefts)
  final def deleteRights: ACursor                = toACursor(cursor.deleteRights, CursorOp.DeleteRights)
  final def setLefts(js: List[Json]): ACursor    = toACursor(cursor.setLefts(js), CursorOp.SetLefts(js))
  final def setRights(js: List[Json]): ACursor   = toACursor(cursor.setRights(js), CursorOp.SetRights(js))
  final def deleteGoField(k: String): ACursor
    = toACursor(cursor.deleteGoField(k), CursorOp.DeleteGoField(k))

  final def as[A](implicit d: Decoder[A]): Decoder.Result[A] = d(this)
  final def get[A](k: String)(implicit d: Decoder[A]): Decoder.Result[A] = downField(k).as[A]

  final def replay(history: List[HistoryOp]): ACursor = ACursor.ok(this).replay(history)
}

final object HCursor {
  /**
   * Create an [[HCursor]] from a [[Cursor]] in order to track history.
   */
  final def fromCursor(cursor: Cursor): HCursor = new HCursor(cursor) {
    final def history: List[HistoryOp] = Nil
  }

  implicit final val eqHCursor: Eq[HCursor] = Eq.instance {
    case (hc1, hc2) => Eq[Cursor].eqv(hc1.cursor, hc2.cursor) && (hc1.history == hc2.history)
  }
}
