package io.circe

import algebra.Eq
import cats.Show
import io.circe.CursorOp._

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

  /** Represents JS style selections into JSON structure */
  private[this] sealed trait Selection
  private[this] case class SelectField(field: String) extends Selection
  private[this] case class SelectIndex(index: Int) extends Selection
  private[this] case class Op(op: CursorOp) extends Selection

  /** Shows history as JS style selections, i.e. ".foo.bar[3]" */
  def opsToPath(history: List[HistoryOp]): String = {

    // Fold into sequence of selections (reducing array ops etc. into single selections)
    val selections = history.foldRight(List[Selection]()) { (historyOp, sels) =>
      (historyOp.op, sels) match {
        case (Some(DownField(k)), _)                   => SelectField(k) :: sels
        case (Some(DownArray), _)                      => SelectIndex(0) :: sels
        case (Some(MoveUp), _ :: rest)                 => rest
        case (Some(MoveRight), SelectIndex(i) :: tail) => SelectIndex(i + 1) :: tail
        case (Some(MoveLeft), SelectIndex(i) :: tail)  => SelectIndex(i - 1) :: tail
        case (Some(RightN(n)), SelectIndex(i) :: tail) => SelectIndex(i + n) :: tail
        case (Some(LeftN(n)), SelectIndex(i) :: tail)  => SelectIndex(i - n) :: tail
        case (Some(op), _)                             => Op(op) :: sels
        case (None, _)                                 => sels
      }
    }

    selections.foldLeft("") {
      case (str, SelectField(f)) => s".$f$str"
      case (str, SelectIndex(i)) => s"[$i]$str"
      case (str, Op(op))         => s"{${Show[CursorOp].show(op)}}$str"
    }
  }
}
