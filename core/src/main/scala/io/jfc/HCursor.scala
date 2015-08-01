package io.jfc

import cats.data.Xor
import io.jfc.cursor.HCursorOperations

case class HCursor(cursor: Cursor, history: CursorHistory) extends HCursorOperations {
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
    f: (A, HCursor) => Xor[DecodeFailure, A]
  ): Xor[DecodeFailure, A] = loop[(HCursor, A), A](
    f(init, this).map(a => (this, a)),
    { case (c, acc) =>
        op(c).success.fold[Xor[Xor[DecodeFailure, A], Xor[DecodeFailure, (HCursor, A)]]](
          Xor.left(Xor.right[DecodeFailure, A](acc))
        )(hcursor =>
          Xor.right(f(acc, hcursor).map(b => (hcursor, b)))
        )
    }
  )

  private[this] final def loop[A, B](
    r1: Xor[DecodeFailure, A],
    f: A => Xor[Xor[DecodeFailure, B], Xor[DecodeFailure, A]]
  ): Xor[DecodeFailure, B] =
    r1.flatMap(a => f(a).swap.valueOr(r2 => loop[A, B](r2, f)))
}
