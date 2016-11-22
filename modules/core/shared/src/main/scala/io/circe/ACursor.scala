package io.circe

import cats.{ Applicative, Eq }
import cats.data.Validated
import cats.instances.either._
import io.circe.ast.Json

/**
 * A cursor that tracks history and represents the possibility of failure.
 *
 * @groupname Ungrouped ACursor fields and operations
 * @groupprio Ungrouped 1
 *
 * @see [[GenericCursor]]
 * @author Travis Brown
 */
sealed abstract class ACursor(final val any: HCursor) extends GenericCursor[ACursor] {
  type Focus[x] = Option[x]
  type Result = ACursor
  type M[x[_]] = Applicative[x]

  final def either: Either[HCursor, HCursor] = if (succeeded) Right(any) else Left(any)

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
  final def reattempt: ACursor = if (succeeded) this else ACursor.ok(any.reattempted)

  /**
   * Return the previous focus, if and only if we didn't succeed.
   */
  final def failureFocus: Option[Json] = failure.map(_.focus)

  /**
   * Return the current cursor or the given one if this one isn't successful.
   */
  final def or(c: => ACursor): ACursor = if (succeeded) this else c

  /**
   * Return a [[cats.data.Validated]] of the underlying cursor.
   */
  final def validation: Validated[HCursor, HCursor] = Validated.fromEither(either)

  final def focus: Option[Json] = success.map(_.focus)
  final def top: Option[Json] = success.map(_.top)

  final def withFocus(f: Json => Json): ACursor =
    if (this.succeeded) ACursor.ok(this.any.withFocus(f)) else this

  final def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor] =
    either.fold(
      _ => F.pure(this),
      valid => F.map(valid.withFocusM(f))(ACursor.ok)
    )

  final def lefts: Option[List[Json]]     = success.flatMap(_.lefts)
  final def rights: Option[List[Json]]    = success.flatMap(_.rights)

  final def fieldSet: Option[Set[String]] = success.flatMap(_.fieldSet)
  final def fields: Option[List[String]]  = success.flatMap(_.fields)

  final def delete: ACursor                      = if (failed) this else any.delete
  final def up: ACursor                          = if (failed) this else any.up
  final def left: ACursor                        = if (failed) this else any.left
  final def right: ACursor                       = if (failed) this else any.right
  final def first: ACursor                       = if (failed) this else any.first
  final def last: ACursor                        = if (failed) this else any.last
  final def leftN(n: Int): ACursor               = if (failed) this else any.leftN(n)
  final def rightN(n: Int): ACursor              = if (failed) this else any.rightN(n)
  final def leftAt(p: Json => Boolean): ACursor  = if (failed) this else any.leftAt(p)
  final def rightAt(p: Json => Boolean): ACursor = if (failed) this else any.rightAt(p)
  final def find(p: Json => Boolean): ACursor    = if (failed) this else any.find(p)
  final def downArray: ACursor                   = if (failed) this else any.downArray
  final def downAt(p: Json => Boolean): ACursor  = if (failed) this else any.downAt(p)
  final def downN(n: Int): ACursor               = if (failed) this else any.downN(n)
  final def field(k: String): ACursor            = if (failed) this else any.field(k)
  final def downField(k: String): ACursor        = if (failed) this else any.downField(k)
  final def deleteGoLeft: ACursor                = if (failed) this else any.deleteGoLeft
  final def deleteGoRight: ACursor               = if (failed) this else any.deleteGoRight
  final def deleteGoFirst: ACursor               = if (failed) this else any.deleteGoFirst
  final def deleteGoLast: ACursor                = if (failed) this else any.deleteGoLast
  final def deleteLefts: ACursor                 = if (failed) this else any.deleteLefts
  final def deleteRights: ACursor                = if (failed) this else any.deleteRights
  final def setLefts(x: List[Json]): ACursor     = if (failed) this else any.setLefts(x)
  final def setRights(x: List[Json]): ACursor    = if (failed) this else any.setRights(x)
  final def deleteGoField(k: String): ACursor    = if (failed) this else any.deleteGoField(k)

  final def as[A](implicit d: Decoder[A]): Decoder.Result[A] = d.tryDecode(this)
  final def get[A](k: String)(implicit d: Decoder[A]): Decoder.Result[A] = downField(k).as[A]

  final def replay(history: List[HistoryOp]): ACursor = history.map(_.op).foldRight(this) {
    case (Some(CursorOp.MoveLeft), acc) => acc.left
    case (Some(CursorOp.MoveRight), acc) => acc.right
    case (Some(CursorOp.MoveFirst), acc) => acc.first
    case (Some(CursorOp.MoveLast), acc) => acc.last
    case (Some(CursorOp.MoveUp), acc) => acc.up
    case (Some(CursorOp.LeftN(n)), acc) => acc.leftN(n)
    case (Some(CursorOp.RightN(n)), acc) => acc.rightN(n)
    case (Some(CursorOp.LeftAt(p)), acc) => acc.leftAt(p)
    case (Some(CursorOp.RightAt(p)), acc) => acc.rightAt(p)
    case (Some(CursorOp.Find(p)), acc) => acc.find(p)
    case (Some(CursorOp.Field(k)), acc) => acc.field(k)
    case (Some(CursorOp.DownField(k)), acc) => acc.downField(k)
    case (Some(CursorOp.DownArray), acc) => acc.downArray
    case (Some(CursorOp.DownAt(p)), acc) => acc.downAt(p)
    case (Some(CursorOp.DownN(n)), acc) => acc.downN(n)
    case (Some(CursorOp.DeleteGoParent), acc) => acc.delete
    case (Some(CursorOp.DeleteGoLeft), acc) => acc.deleteGoLeft
    case (Some(CursorOp.DeleteGoRight), acc) => acc.deleteGoRight
    case (Some(CursorOp.DeleteGoFirst), acc) => acc.deleteGoFirst
    case (Some(CursorOp.DeleteGoLast), acc) => acc.deleteGoLast
    case (Some(CursorOp.DeleteGoField(k)), acc) => acc.deleteGoField(k)
    case (Some(CursorOp.DeleteLefts), acc) => acc.deleteLefts
    case (Some(CursorOp.DeleteRights), acc) => acc.deleteRights
    case (Some(CursorOp.SetLefts(js)), acc) => acc.setLefts(js)
    case (Some(CursorOp.SetRights(js)), acc) => acc.setRights(js)
    case (None, acc) => acc
  }
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
