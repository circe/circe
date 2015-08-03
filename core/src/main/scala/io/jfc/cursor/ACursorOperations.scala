package io.jfc.cursor

import cats.Applicative
import cats.data.Xor
import io.jfc.{ ACursor, Decode, DecodeFailure, GenericCursor, HCursor, Json }

/**
 * A helper trait that implements cursor operations for [[io.jfc.ACursor]].
 */
private[jfc] trait ACursorOperations extends GenericCursor[ACursor] { this: ACursor =>
  type Focus[x] = Option[x]
  type Result = ACursor
  type M[x[_]] = Applicative[x]

  /**
   * A helper method to simplify performing operations on the underlying
   * [[HCursor]].
   */
  private[this] def withHCursor(f: HCursor => ACursor): ACursor =
    ACursor(either.flatMap(c => f(c).either))

  def focus: Option[Json] = success.map(_.focus)
  def undo: Option[Json] = success.map(_.undo)
  def up: ACursor = withHCursor(_.up)

  def delete: ACursor = withHCursor(_.delete)
  def withFocus(f: Json => Json): ACursor = ACursor(either.map(_.withFocus(f)))
  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor] =
    either.fold(
      _ => F.pure(this),
      valid => F.map(valid.withFocusM(f))(ACursor.ok)
    )

  def lefts: Option[List[Json]] = success.flatMap(_.lefts)
  def rights: Option[List[Json]] = success.flatMap(_.rights)

  def fieldSet: Option[Set[String]] = success.flatMap(_.fieldSet)
  def fields: Option[List[String]] = success.flatMap(_.fields)

  def left: ACursor = withHCursor(_.left)
  def right: ACursor = withHCursor(_.right)
  def first: ACursor = withHCursor(_.first)
  def last: ACursor = withHCursor(_.last)
  def leftN(n: Int): ACursor = withHCursor(_.leftN(n))
  def rightN(n: Int): ACursor = withHCursor(_.rightN(n))
  def leftAt(p: Json => Boolean): ACursor = withHCursor(_.leftAt(p))
  def rightAt(p: Json => Boolean): ACursor = withHCursor(_.rightAt(p))
  def find(p: Json => Boolean): ACursor = withHCursor(_.find(p))
  def downArray: ACursor = withHCursor(_.downArray)
  def downAt(p: Json => Boolean): ACursor = withHCursor(_.downAt(p))
  def downN(n: Int): ACursor = withHCursor(_.downN(n))

  def field(k: String): ACursor = withHCursor(_.field(k))
  def downField(k: String): ACursor = withHCursor(_.downField(k))

  def deleteGoLeft: ACursor = withHCursor(_.deleteGoLeft)
  def deleteGoRight: ACursor = withHCursor(_.deleteGoRight)
  def deleteGoFirst: ACursor = withHCursor(_.deleteGoFirst)
  def deleteGoLast: ACursor = withHCursor(_.deleteGoLast)
  def deleteLefts: ACursor = withHCursor(_.deleteLefts)
  def deleteRights: ACursor = withHCursor(_.deleteRights)
  def setLefts(x: List[Json]): ACursor = withHCursor(_.setLefts(x))
  def setRights(x: List[Json]): ACursor = withHCursor(_.setRights(x))

  def deleteGoField(k: String): ACursor = withHCursor(_.deleteGoField(k))

  def as[A](implicit decode: Decode[A]): Xor[DecodeFailure, A] = decode.tryDecode(this)
  def get[A](k: String)(implicit decode: Decode[A]): Xor[DecodeFailure, A] = downField(k).as[A]
}
