package io.circe

import cats.syntax.all._
import cats._
import scala.annotation.tailrec

abstract class HCursor(evalLastCursor: Eval[HCursor], lastOp: CursorOp) extends ACursor(evalLastCursor, lastOp) {
  def value: Json

  def replace(newValue: Json, cursor: HCursor, op: CursorOp): ACursor

  // TODO: This only works on arrays, unless the current focus is already
  // valid, which is really odd. This should probably be updated.
  final def find(p: Json => Boolean): ACursor = {
    @tailrec
    def go(c: ACursor): ACursor = c match {
      case success: HCursor => if (p(success.value)) success else go(success.right)
      case other            => other
    }

    go(this)
  }
}

object HCursor {
  def fromJson(value: Json): HCursor =
    fromCursor(Cursor.fromJson(value))

  def fromCursor(cursor: Cursor.SuccessCursor): HCursor =
    new HCursor(Eval.later(cursor.lastCursor.fold(null: HCursor)(fromCursor)), cursor.lastOp.getOrElse(null)) {
      override def delete: ACursor =
        ACursor.fromCursor(cursor.delete)

      override def field(k: String): ACursor =
        ACursor.fromCursor(cursor.field(k))

      override def left: ACursor =
        ACursor.fromCursor(cursor.left)

      override def right: ACursor =
        ACursor.fromCursor(cursor.right)

      override def up: ACursor =
        ACursor.fromCursor(cursor.up)

      override def downArray: ACursor =
        ACursor.fromCursor(cursor.downArray)

      override def downField(k: String): ACursor =
        ACursor.fromCursor(cursor.downField(k))

      override def downN(n: Int): ACursor =
        ACursor.fromCursor(cursor.downN(n))

      override def focus: Option[Json] =
        cursor.focus

      override def keys: Option[Iterable[String]] =
        cursor.keys

      override def succeeded: Boolean =
        cursor.succeeded

      override def success: Option[HCursor] =
        Some(this)

      override def top: Option[Json] =
        cursor.top

      override def values: Option[Iterable[Json]] =
        cursor.values

      override def withFocus(f: Json => Json): ACursor =
        ACursor.fromCursor(cursor.mapFocus(f))

      override def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor] =
        cursor.mapFocusA[F](f).map(ACursor.fromCursor)

      override def replace(newValue: Json, c: HCursor, op: CursorOp): ACursor =
        ACursor.fromCursor(cursor.set(newValue))

      override def value: Json =
        cursor.value

      override def history: List[CursorOp] =
        cursor.history.asList

      override def index: Option[Int] =
        cursor.index

      override def key: Option[String] =
        cursor.key

      // Most operations on a FailedCursor are terminal, but for some unknown
      // reason, .root will get you out of a failed state.
      //
      // It is my opinion we should not do this in io.circe.Cursor, but it is
      // emulated here for backwards compatibility.
      override def root: HCursor =
        (cursor match {
          case cursor if cursor.failed =>
            cursor.lastCursor.fold(cursor: Cursor)(_.root)
          case _ =>
            cursor.root
        }) match {
          case cursor: Cursor.SuccessCursor =>
            HCursor.fromCursor(cursor)
          case _ =>
            this
        }

      override def replayOne(op: CursorOp): ACursor =
        ACursor.fromCursor(cursor.replayOne(op))

      override def replay(history: List[CursorOp]): ACursor =
        ACursor.fromCursor(cursor.replayCursorOps(history.reverse))

      override final def toString: String =
        cursor.toString

      override final def pathToRoot: PathToRoot =
        PathToRoot.fromCursorPath(cursor.cursorPath)
    }
}
