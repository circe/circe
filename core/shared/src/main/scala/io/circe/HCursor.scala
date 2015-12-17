package io.circe

import algebra.Eq
import cats.data.{ NonEmptyList, Validated, Xor }
import io.circe.cursor.HCursorOperations

import scala.annotation.tailrec

/**
 * A cursor that tracks the history of operations performed with it.
 *
 * @groupname Ungrouped HCursor fields and operations
 * @groupprio Ungrouped 1
 *
 * @see [[GenericCursor]]
 * @author Travis Brown
 */
abstract class HCursor private[circe](val cursor: Cursor) extends HCursorOperations {
  def history: List[HistoryOp]

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
    f: (A, HCursor) => Decoder.Result[A]
  ): Decoder.Result[A] = loop[(HCursor, A), A](
    f(init, this).map(a => (this, a)),
    { case (c, acc) =>
        op(c).success.fold[Xor[Decoder.Result[A], Decoder.Result[(HCursor, A)]]](
          Xor.left(Xor.right[DecodingFailure, A](acc))
        )(hcursor =>
          Xor.right(f(acc, hcursor).map(b => (hcursor, b)))
        )
    }
  )

  @tailrec private[this] final def loop[A, B](
    r1: Decoder.Result[A],
    f: A => Xor[Decoder.Result[B], Decoder.Result[A]]
  ): Decoder.Result[B] =
    r1 match {
      case l @ Xor.Left(_) => l
      case Xor.Right(a) => f(a) match {
        case Xor.Left(b) => b
        case Xor.Right(r2) => loop[A, B](r2, f)
      }
    }

  /**
   * Traverse taking `op` at each step, performing `f` on the current cursor and
   * accumulating `A`.
   *
   * This operation does not consume stack at each step, so is safe to work with
   * large structures (in contrast with recursively binding).
   */
  @tailrec final def traverseDecodeAccumulating[A](init: AccumulatingDecoder.Result[A])(
    op: HCursor => ACursor,
    f: (AccumulatingDecoder.Result[A], HCursor) => AccumulatingDecoder.Result[A]
  ): AccumulatingDecoder.Result[A] =
    op(this).success match {
      case None => f(init, this)
      case Some(next) => next.traverseDecodeAccumulating(f(init, this))(op, f)
    }
}

object HCursor {
  implicit val eqHCursor: Eq[HCursor] = Eq.instance {
    case (hc1, hc2) => Eq[Cursor].eqv(hc1.cursor, hc2.cursor) && (hc1.history == hc2.history)
  }
}
