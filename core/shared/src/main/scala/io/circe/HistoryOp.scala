package io.circe

import algebra.Eq
import cats.Show

sealed abstract class HistoryOp extends Product with Serializable {
  def isReattempt: Boolean
  def isNotReattempt: Boolean
  def succeeded: Boolean
  def failed: Boolean
  def op: Option[CursorOp]
}

final object HistoryOp {
  final def ok(o: CursorOp): HistoryOp = El(o, succeeded = true)
  final def fail(o: CursorOp): HistoryOp = El(o, succeeded = false)
  final def reattempt: HistoryOp = Reattempt

  private[this] final case object Reattempt extends HistoryOp {
    final def isReattempt: Boolean = true
    final def isNotReattempt: Boolean = false
    final def succeeded: Boolean = false
    final def failed: Boolean = false
    final def op: Option[CursorOp] = None
  }

  private[this] final case class El(o: CursorOp, succeeded: Boolean) extends HistoryOp {
    final def isReattempt: Boolean = false
    final def isNotReattempt: Boolean = true
    final def failed: Boolean = !succeeded
    final def op: Option[CursorOp] = Some(o)
  }

  implicit final val eqCursorOp: Eq[HistoryOp] = Eq.instance {
    case (Reattempt, Reattempt) => true
    case (El(o1, s1), El(o2, s2)) => Eq[CursorOp].eqv(o1, o2) && s1 == s2
    case (_, _) => false
  }

  implicit final val showCursorOp: Show[HistoryOp] = Show.show {
    case Reattempt => ".?."
    case El(o, s) =>
      val shownOp = Show[CursorOp].show(o)
      if (s) shownOp else s"*.$shownOp"
  }
}
