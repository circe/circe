package io.jfc

import cats.Applicative
import cats.data.{ Validated, Xor }
import io.jfc.cursor.ACursorOperations

/**
 * A cursor that tracks history and represents the possibility of failure.
 */
case class ACursor(either: Xor[HCursor, HCursor]) extends ACursorOperations {
  /**
   * Return the current [[HCursor]] if we are in a sucsess state.
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
   * Return the underlying cursor.
   */
  def cursor: Option[Cursor] = success.map(_.cursor)
  def any: HCursor = either.merge
  def history: CursorHistory = any.history

  def reattempt: ACursor = either.fold(
    invalid => ACursor.ok(new HCursor(invalid.cursor, CursorOp.reattemptOp +: invalid.history)),
    _ => this
  )

  /** Return the current focus, iff we are succeeded */

  /** Return the previous focus, iff we are !succeeded. */
  def failureFocus: Option[Json] = failure.map(_.focus)

  /** Update the focus with the given function (alias for `>->`). */

  /** Update the focus with the given function in a applicative (alias for `>-->`). */

  /**
   * Return the values left of focus in a JSON array.
   */
  def lefts: Option[List[Json]] = success.flatMap(_.lefts)

  /**
   * Return the values right of focus in a JSON array.
   */
  def rights: Option[List[Json]] = success.flatMap(_.rights)

  def fieldSet: Option[Set[String]] = success.flatMap(_.fieldSet)
  def fields: Option[List[String]] = success.flatMap(_.fields)

  def |||(c: => ACursor): ACursor = if (succeeded) this else c

  def validation: Validated[HCursor, HCursor] = either.toValidated
}

object ACursor {
  def ok(cursor: HCursor) = ACursor(Xor.right(cursor))
  def fail(cursor: HCursor) = ACursor(Xor.left(cursor))
}
