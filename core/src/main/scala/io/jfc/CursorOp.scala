package io.jfc

import algebra.Eq
import cats.Show

sealed abstract class CursorOp extends Product with Serializable {
  def isReattempt: Boolean
  def isNotReattempt: Boolean
  def succeeded: Boolean
  def failed: Boolean
  def op: Option[CursorOpElement]
}

object CursorOp {
  def ok(o: CursorOpElement): CursorOp = El(o, succeeded = true)
  def fail(o: CursorOpElement): CursorOp = El(o, succeeded = false)
  def reattempt: CursorOp = Reattempt

  private[this] case object Reattempt extends CursorOp {
    def isReattempt: Boolean = true
    def isNotReattempt: Boolean = false
    def succeeded: Boolean = false
    def failed: Boolean = false
    def op: Option[CursorOpElement] = None
  }

  private[this] case class El(o: CursorOpElement, succeeded: Boolean) extends CursorOp {
    def isReattempt: Boolean = false
    def isNotReattempt: Boolean = true
    def failed: Boolean = !succeeded
    def op: Option[CursorOpElement] = Some(o)
  }

  implicit val eqCursorOp: Eq[CursorOp] = Eq.instance {
    case (Reattempt, Reattempt) => true
    case (El(o1, s1), El(o2, s2)) => Eq[CursorOpElement].eqv(o1, o2) && s1 == s2
    case (_, _) => false
  }

  implicit val showCursorOp: Show[CursorOp] = Show.show {
    case Reattempt => ".?."
    case El(o, s) =>
      val shownOp = Show[CursorOpElement].show(o)
      if (s) shownOp else s"*.$shownOp"
  }
}
