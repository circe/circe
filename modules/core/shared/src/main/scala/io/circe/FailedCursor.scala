package io.circe

import cats.Applicative
import cats.kernel.Eq

abstract class FailedCursor(val incorrectFocus: Boolean) extends ACursor {
  final def succeeded: Boolean = false
  final def success: Option[HCursor] = None

  final def focus: Option[Json] = None
  final def top: Option[Json] = None

  final def withFocus(f: Json => Json): ACursor = this
  final def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor] = F.pure(this)

  final def fieldSet: Option[Set[String]] = None
  final def fields: Option[List[String]] = None
  final def lefts: Option[List[Json]] = None
  final def rights: Option[List[Json]] = None

  final def downArray: ACursor = this
  final def downAt(p: Json => Boolean): ACursor = this
  final def downField(k: String): ACursor = this
  final def downN(n: Int): ACursor = this
  final def find(p: Json => Boolean): ACursor = this
  final def leftAt(p: Json => Boolean): ACursor = this
  final def leftN(n: Int): ACursor = this
  final def rightAt(p: Json => Boolean): ACursor = this
  final def rightN(n: Int): ACursor = this
  final def up: ACursor = this

  final def left: ACursor = this
  final def right: ACursor = this
  final def first: ACursor = this
  final def last: ACursor = this

  final def delete: ACursor = this
  final def deleteGoLeft: ACursor = this
  final def deleteGoRight: ACursor = this
  final def deleteGoFirst: ACursor = this
  final def deleteGoLast: ACursor = this
  final def deleteLefts: ACursor = this
  final def deleteRights: ACursor = this

  final def setLefts(x: List[Json]): ACursor = this
  final def setRights(x: List[Json]): ACursor = this

  final def field(k: String): ACursor = this
  final def deleteGoField(q: String): ACursor = this
}

final object FailedCursor {
  implicit val eqFailedCursor: Eq[FailedCursor] =
    cats.instances.list.catsKernelStdEqForList[CursorOp].on[FailedCursor](_.history)
}
