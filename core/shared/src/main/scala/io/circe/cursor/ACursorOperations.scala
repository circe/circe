package io.circe.cursor

import cats.Applicative
import cats.data.Xor
import io.circe._

/**
 * A helper trait that implements cursor operations for [[io.circe.ACursor]].
 */
private[circe] trait ACursorOperations extends GenericCursor[ACursor] { this: ACursor =>
  type Focus[x] = Option[x]
  type Result = ACursor
  type M[x[_]] = Applicative[x]

  /**
   * A helper method to simplify performing operations on the underlying [[HCursor]].
   */
  private[this] def withHCursor(f: HCursor => ACursor): ACursor =
    if (this.succeeded) f(this.any) else this

  def focus: Option[Json] = success.map(_.focus)
  def top: Option[Json] = success.map(_.top)

  def delete: ACursor = withHCursor(_.delete)
  def withFocus(f: Json => Json): ACursor =
    if (this.succeeded) ACursor.ok(this.any.withFocus(f)) else this

  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor] =
    either.fold(
      _ => F.pure(this),
      valid => F.map(valid.withFocusM(f))(ACursor.ok)
    )

  def lefts: Option[List[Json]]     = success.flatMap(_.lefts)
  def rights: Option[List[Json]]    = success.flatMap(_.rights)

  def fieldSet: Option[Set[String]] = success.flatMap(_.fieldSet)
  def fields: Option[List[String]]  = success.flatMap(_.fields)

  def up: ACursor                          = withHCursor(_.up)
  def left: ACursor                        = withHCursor(_.left)
  def right: ACursor                       = withHCursor(_.right)
  def first: ACursor                       = withHCursor(_.first)
  def last: ACursor                        = withHCursor(_.last)
  def leftN(n: Int): ACursor               = withHCursor(_.leftN(n))
  def rightN(n: Int): ACursor              = withHCursor(_.rightN(n))
  def leftAt(p: Json => Boolean): ACursor  = withHCursor(_.leftAt(p))
  def rightAt(p: Json => Boolean): ACursor = withHCursor(_.rightAt(p))
  def find(p: Json => Boolean): ACursor    = withHCursor(_.find(p))
  def downArray: ACursor                   = withHCursor(_.downArray)
  def downAt(p: Json => Boolean): ACursor  = withHCursor(_.downAt(p))
  def downN(n: Int): ACursor               = withHCursor(_.downN(n))
  def field(k: String): ACursor            = withHCursor(_.field(k))
  def downField(k: String): ACursor        = withHCursor(_.downField(k))
  def deleteGoLeft: ACursor                = withHCursor(_.deleteGoLeft)
  def deleteGoRight: ACursor               = withHCursor(_.deleteGoRight)
  def deleteGoFirst: ACursor               = withHCursor(_.deleteGoFirst)
  def deleteGoLast: ACursor                = withHCursor(_.deleteGoLast)
  def deleteLefts: ACursor                 = withHCursor(_.deleteLefts)
  def deleteRights: ACursor                = withHCursor(_.deleteRights)
  def setLefts(x: List[Json]): ACursor     = withHCursor(_.setLefts(x))
  def setRights(x: List[Json]): ACursor    = withHCursor(_.setRights(x))
  def deleteGoField(k: String): ACursor    = withHCursor(_.deleteGoField(k))

  def as[A](implicit d: Decoder[A]): Decoder.Result[A] = d.tryDecode(this)
  def get[A](k: String)(implicit d: Decoder[A]): Decoder.Result[A] = downField(k).as[A]

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
