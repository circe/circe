package io.circe

import algebra.Eq
import cats.data.{ Validated, Xor }
import io.circe.cursor.ACursorOperations

/**
 * A cursor that tracks history and represents the possibility of failure.
 *
 * @groupname Ungrouped ACursor fields and operations
 * @groupprio Ungrouped 1
 *
 * @see [[GenericCursor]]
 * @author Travis Brown
 */
abstract class ACursor private[circe](val any: HCursor) extends ACursorOperations {
  final def either: Xor[HCursor, HCursor] = if (succeeded) Xor.right(any) else Xor.left(any)

  /**
   * Return the current [[HCursor]] if we are in a success state.
   */
  final def success: Option[HCursor] = if (succeeded) Some(any) else None

  /**
   * Return the failed [[HCursor]] if we are in a failure state.
   */
  final def failure: Option[HCursor] = if (succeeded) None else Some(any)

  /**
   * Indicate whether this cursor represents the result of a successful
   * operation.
   */
  def succeeded: Boolean

  /**
   * Indicate whether this cursor represents the result of an unsuccessful
   * operation.
   */
  final def failed: Boolean = !succeeded

  /**
   * Return the underlying cursor if successful.
   */
  final def cursor: Option[Cursor] = success.map(_.cursor)

  /**
   * Return the underlying cursor's history.
   */
  final def history: List[HistoryOp] = any.history

  /**
   * If the last operation was not successful, reattempt it.
   */
  final def reattempt: ACursor = if (succeeded) this else ACursor.ok(
    new HCursor(any.cursor) {
      final def history: List[HistoryOp] = HistoryOp.reattempt +: any.history
    }
  )

  /**
   * Return the previous focus, if and only if we didn't succeed.
   */
  final def failureFocus: Option[Json] = failure.map(_.focus)

  /**
   * Return the current cursor or the given one if this one isn't successful.
   */
  final def or(c: => ACursor): ACursor = if (succeeded) this else c

  /**
   * Return the current cursor or the given one if this one isn't successful.
   */
  @deprecated("Use or", "0.3.0")
  final def |||(c: => ACursor): ACursor = or(c)

  /**
   * Return a [[cats.data.Validated]] of the underlying cursor.
   */
  final def validation: Validated[HCursor, HCursor] = either.toValidated
}

final object ACursor {
  final def ok(cursor: HCursor): ACursor = new ACursor(cursor) {
    def succeeded: Boolean = true
  }

  final def fail(cursor: HCursor): ACursor = new ACursor(cursor) {
    def succeeded: Boolean = false
  }

  implicit final val eqACursor: Eq[ACursor] = Eq.by(_.either)
}
