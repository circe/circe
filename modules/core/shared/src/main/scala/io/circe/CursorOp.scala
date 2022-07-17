package io.circe

import cats.{ Eq, Show }
import cats.syntax.all._
import java.io.Serializable

sealed abstract class CursorOp extends Product with Serializable {
  import CursorOp._

  /**
   * Does this operation require the current focus (not context) to be an
   * object?
   */
  final def requiresObject: Boolean =
    this match {
      case _: DownField =>
        true
      case _ =>
        false
    }

  /**
   * Does this operation require the current focus (not context) to be an array?
   */
  final def requiresArray: Boolean =
    this match {
      case _: DownArray.type | _: DownN => true
      case _                            => false
    }

  override final def toString: String =
    this.show
}

object CursorOp {
  case object MoveLeft extends CursorOp
  case object MoveRight extends CursorOp
  case object MoveUp extends CursorOp
  final case class Field(k: String) extends CursorOp
  final case class DownField(k: String) extends CursorOp
  case object DownArray extends CursorOp
  final case class DownN(n: Int) extends CursorOp
  case object DeleteGoParent extends CursorOp
  final case class Replace(value: Json) extends CursorOp

  // TODO: Should we keep this? I'm not sure it is useful.
  implicit final val showCursorOp: Show[CursorOp] = Show.show {
    case MoveLeft       => "<-"
    case MoveRight      => "->"
    case MoveUp         => "_/"
    case Field(f)       => "--(" + f + ")"
    case DownField(f)   => "--\\(" + f + ")"
    case DownArray      => "\\\\"
    case DownN(n)       => "=\\(" + n + ")"
    case DeleteGoParent => "!_/"
    case Replace(_)     => "~"
  }

  implicit final val eqCursorOp: Eq[CursorOp] = Eq.fromUniversalEquals

  val eqCursorOpList: Eq[List[CursorOp]] = cats.instances.list.catsKernelStdEqForList[CursorOp]

  /**
   * Represents JavaScript-style selections into a JSON structure.
   */
  private[this] sealed trait Selection
  private[this] case class SelectField(field: String) extends Selection
  private[this] case class SelectIndex(index: Int) extends Selection
  private[this] case class Op(op: CursorOp) extends Selection

  /** Shows history as JS style selections, i.e. ".foo.bar[3]" */
  def opsToPath(history: List[CursorOp]): String = {
    // Fold into sequence of selections (reducing array ops etc. into single selections)
    val selections = history.foldRight(List.empty[Selection]) {
      case (DownField(k), acc)                 => SelectField(k) :: acc
      case (DownArray, acc)                    => SelectIndex(0) :: acc
      case (MoveUp, _ :: tail)                 => tail
      case (MoveRight, SelectIndex(i) :: tail) => SelectIndex(i + 1) :: tail
      case (MoveLeft, SelectIndex(i) :: tail)  => SelectIndex(i - 1) :: tail
      case (op, acc)                           => Op(op) :: acc
    }

    selections.foldLeft("") {
      case (str, SelectField(f)) => s".$f$str"
      case (str, SelectIndex(i)) => s"[$i]$str"
      case (str, Op(op))         => s"{${Show[CursorOp].show(op)}}$str"
    }
  }
}
