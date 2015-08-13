package io.circe

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
case class ACursor(either: Xor[HCursor, HCursor]) extends ACursorOperations {
  /**
   * Return the current [[HCursor]] if we are in a success state.
   */
  def success: Option[HCursor] = either.toOption

  /**
   * Return the failed [[HCursor]] if we are in a failure state.
   */
  def failure: Option[HCursor] = either.swap.toOption

  /**
   * Indicate whether this cursor represents the result of a successful
   * operation.
   */
  def succeeded: Boolean = success.isDefined

  /**
   * Indicate whether this cursor represents the result of an unsuccessful
   * operation.
   */
  def failed: Boolean = !succeeded

  /**
   * Return the underlying cursor if successful.
   */
  def cursor: Option[Cursor] = success.map(_.cursor)

  /**
   * Return the underlying cursor.
   */
  def any: HCursor = either.merge

  /**
   * Return the underlying cursor's history.
   */
  def history: List[CursorOp] = any.history

  /**
   * If the last operation was not successful, reattempt it.
   */
  def reattempt: ACursor = either.fold(
    invalid => ACursor.ok(HCursor(invalid.cursor, CursorOp.reattempt +: invalid.history)),
    _ => this
  )

  /**
   * Return the previous focus, if and only if we didn't succeed.
   */
  def failureFocus: Option[Json] = failure.map(_.focus)

  /**
   * Return the current cursor or the given one if this one isn't successful.
   */
  def |||(c: => ACursor): ACursor = if (succeeded) this else c

  /**
   * Return a [[cats.data.Validated]] of the underlying cursor.
   */
  def validation: Validated[HCursor, HCursor] = either.toValidated
}

object ACursor {
  def ok(cursor: HCursor): ACursor = ACursor(Xor.right(cursor))
  def fail(cursor: HCursor): ACursor = ACursor(Xor.left(cursor))
}
