package io.circe

import cats.Applicative
import scala.collection.immutable.Set

final class FailedCursor(lastCursor: HCursor, lastOp: CursorOp) extends ACursor(lastCursor, lastOp) {
  /**
   * Indicates whether the last operation failed because the type of the focus
   * was wrong.
   */
  def incorrectFocus: Boolean =
    (lastOp.requiresObject && !lastCursor.value.isObject) || (lastOp.requiresArray && !lastCursor.value.isArray)

  def succeeded: Boolean = false
  def success: Option[HCursor] = None

  def focus: Option[Json] = None
  def top: Option[Json] = None

  def withFocus(f: Json => Json): ACursor = this
  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor] = F.pure(this)

  def values: Option[Vector[Json]] = None
  def fieldSet: Option[Set[String]] = None
  def fields: Option[Vector[String]] = None
  def lefts: Option[Vector[Json]] = None
  def rights: Option[Vector[Json]] = None

  def downArray: ACursor = this
  def downAt(p: Json => Boolean): ACursor = this
  def downField(k: String): ACursor = this
  def downN(n: Int): ACursor = this
  def find(p: Json => Boolean): ACursor = this
  def leftAt(p: Json => Boolean): ACursor = this
  def leftN(n: Int): ACursor = this
  def rightAt(p: Json => Boolean): ACursor = this
  def rightN(n: Int): ACursor = this
  def up: ACursor = this

  def left: ACursor = this
  def right: ACursor = this
  def first: ACursor = this
  def last: ACursor = this

  def delete: ACursor = this
  def deleteGoLeft: ACursor = this
  def deleteGoRight: ACursor = this
  def deleteGoFirst: ACursor = this
  def deleteGoLast: ACursor = this
  def deleteLefts: ACursor = this
  def deleteRights: ACursor = this

  def setLefts(x: Vector[Json]): ACursor = this
  def setRights(x: Vector[Json]): ACursor = this

  def field(k: String): ACursor = this
  def deleteGoField(q: String): ACursor = this
}
