package io.circe

import algebra.Eq
import cats.Show

sealed abstract class HistoryOp extends Product with Serializable {
  def isReattempt: Boolean
  def isNotReattempt: Boolean
  def succeeded: Boolean
  def failed: Boolean
  def incorrectFocus: Boolean
  def op: Option[CursorOp]
}

final object HistoryOp {
  final def ok(o: CursorOp): HistoryOp = El(o, true, false)
  final def fail(o: CursorOp, incorrectContext: Boolean): HistoryOp = El(o, false, incorrectContext)
  final def reattempt: HistoryOp = Reattempt

  private[this] final case object Reattempt extends HistoryOp {
    final val isReattempt: Boolean = true
    final val isNotReattempt: Boolean = false
    final val succeeded: Boolean = false
    final val failed: Boolean = false
    final val incorrectFocus: Boolean = false
    final val op: Option[CursorOp] = None
  }

  private[this] final case class El(
    o: CursorOp,
    succeeded: Boolean,
    incorrectFocus: Boolean
  ) extends HistoryOp {
    final def isReattempt: Boolean = false
    final def isNotReattempt: Boolean = true
    final def failed: Boolean = !succeeded
    final def op: Option[CursorOp] = Some(o)
  }

  implicit final val eqCursorOp: Eq[HistoryOp] = Eq.instance {
    case (Reattempt, Reattempt) => true
    case (El(o1, s1, c1), El(o2, s2, c2)) => Eq[CursorOp].eqv(o1, o2) && s1 == s2 && c1 == c2
    case (_, _) => false
  }

  implicit final val showCursorOp: Show[HistoryOp] = Show.show {
    case Reattempt => ".?."
    case El(o, s, _) =>
      val shownOp = Show[CursorOp].show(o)
      if (s) shownOp else s"*.$shownOp"
  }
}
