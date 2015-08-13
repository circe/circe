package io.circe

import cats.data.Validated
import cats.{Applicative, Functor}
import io.circe.cursor.{CJson, CObject, CArray}

trait ExtraGenericCursorOperations extends Any {

  type C <: GenericCursor[C]

  /**
   * The type class including the operations needed for `withFocusM`.
   *
   * @group TypeMembers
   */
  type M[F[_]] <: Functor[F]

  /**
   * Modify the focus in a context using the given function.
   *
   * @group Modification
   */
  def withFocusM[F[_]: M](f: Json => F[Json]): F[C]

}

class ExtraCursorOperations(val cursor: Cursor) extends AnyVal with ExtraGenericCursorOperations {

  type C = Cursor
  type M[x[_]] = Functor[x]

  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Functor[F]): F[Cursor] =
    cursor match {
      case ca: CArray => F.map(f(ca.focus))(j => ca.copy(focus = j, u = true))
      case co: CObject => F.map(f(co.focus))(j => co.copy(focus = j, u = true))
      case cj: CJson => F.map(f(cj.focus))(CJson.apply)
    }

}

class ExtraACursorOperations(val cursor: ACursor) extends AnyVal with ExtraGenericCursorOperations {

  type C = ACursor
  type M[x[_]] = Applicative[x]

  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor] =
    cursor.either.fold(
      _ => F.pure(cursor),
      valid => F.map(new ExtraHCursorOperations(valid).withFocusM(f))(ACursor.ok)
    )

  /**
   * Return a [[cats.data.Validated]] of the underlying cursor.
   */
  def validation: Validated[HCursor, HCursor] = Validated.fromEither(cursor.either)

}

class ExtraHCursorOperations(val cursor: HCursor) extends AnyVal with ExtraGenericCursorOperations {

  type C = HCursor
  type M[x[_]] = Functor[x]

  def withFocusM[F[_]: Functor](f: Json => F[Json]): F[HCursor] =
    Functor[F].map(new ExtraCursorOperations(cursor.cursor).withFocusM(f))(c =>
      HCursor(c, cursor.history)
    )

}
