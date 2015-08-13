package io.circe

import io.circe.cursor.HCursorOperations

/**
 * A cursor that tracks the history of operations performed with it.
 *
 * @groupname Ungrouped HCursor fields and operations
 * @groupprio Ungrouped 1
 *
 * @see [[GenericCursor]]
 * @author Travis Brown
 */
case class HCursor(cursor: Cursor, history: List[CursorOp]) extends HCursorOperations {
  /**
   * Create an [[ACursor]] for this cursor.
   */
  def acursor: ACursor = ACursor.ok(this)

  /**
   * Create a failed [[ACursor]] for this cursor.
   */
  def failedACursor: ACursor = ACursor.fail(this)

  /**
   * Traverse taking `op` at each step, performing `f` on the current cursor and
   * accumulating `A`.
   *
   * This operation does not consume stack at each step, so is safe to work with
   * large structures (in contrast with recursively binding).
   */
  def traverseDecode[A](init: A)(
    op: HCursor => ACursor,
    f: (A, HCursor) => Either[DecodingFailure, A]
  ): Either[DecodingFailure, A] = loop[(HCursor, A), A](
    f(init, this).right.map(a => (this, a)),
    { case (c, acc) =>
        op(c)
          .success
          .fold[Either[Either[DecodingFailure, A], Either[DecodingFailure, (HCursor, A)]]](
            Left(Right[DecodingFailure, A](acc))
          )(hcursor =>
            Right(f(acc, hcursor).right.map(b => (hcursor, b)))
          )
    }
  )

  private[this] final def loop[A, B](
    r1: Either[DecodingFailure, A],
    f: A => Either[Either[DecodingFailure, B], Either[DecodingFailure, A]]
  ): Either[DecodingFailure, B] =
    r1.right.flatMap(a => f(a).swap.left.map(r2 => loop[A, B](r2, f)).merge)
}
