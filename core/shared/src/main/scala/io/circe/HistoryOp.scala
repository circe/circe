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

object HistoryOp {
  def ok(o: CursorOp): HistoryOp = El(o, succeeded = true)
  def fail(o: CursorOp): HistoryOp = El(o, succeeded = false)
  def reattempt: HistoryOp = Reattempt

  private[this] case object Reattempt extends HistoryOp {
    def isReattempt: Boolean = true
    def isNotReattempt: Boolean = false
    def succeeded: Boolean = false
    def failed: Boolean = false
    def op: Option[CursorOp] = None
  }

  private[this] case class El(o: CursorOp, succeeded: Boolean) extends HistoryOp {
    def isReattempt: Boolean = false
    def isNotReattempt: Boolean = true
    def failed: Boolean = !succeeded
    def op: Option[CursorOp] = Some(o)
  }

  implicit val eqCursorOp: Eq[HistoryOp] = Eq.instance {
    case (Reattempt, Reattempt) => true
    case (El(o1, s1), El(o2, s2)) => Eq[CursorOp].eqv(o1, o2) && s1 == s2
    case (_, _) => false
  }

  implicit val showCursorOp: Show[HistoryOp] = Show.show {
    case Reattempt => ".?."
    case El(o, s) =>
      val shownOp = Show[CursorOp].show(o)
      if (s) shownOp else s"*.$shownOp"
  }
}
