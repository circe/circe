package io.circe

import cats.implicits.toShow
import cats.{ Eq, Show }

import java.io.Serializable
import scala.annotation.tailrec

sealed abstract class CursorOp extends Product with Serializable {

  /**
   * Does this operation require the current focus (not context) to be an
   * object?
   */
  def requiresObject: Boolean

  /**
   * Does this operation require the current focus (not context) to be an array?
   */
  def requiresArray: Boolean
}

object CursorOp {
  abstract sealed class ObjectOp extends CursorOp {
    final def requiresObject: Boolean = true
    final def requiresArray: Boolean = false
  }

  abstract sealed class ArrayOp extends CursorOp {
    final def requiresObject: Boolean = false
    final def requiresArray: Boolean = true
  }

  abstract sealed class UnconstrainedOp extends CursorOp {
    final def requiresObject: Boolean = false
    final def requiresArray: Boolean = false
  }

  case object MoveLeft extends UnconstrainedOp
  case object MoveRight extends UnconstrainedOp
  case object MoveUp extends UnconstrainedOp
  final case class Field(k: String) extends UnconstrainedOp
  final case class DownField(k: String) extends ObjectOp
  case object DownArray extends ArrayOp
  final case class DownN(n: Int) extends ArrayOp
  case object DeleteGoParent extends UnconstrainedOp

  implicit final val showCursorOp: Show[CursorOp] = Show.show {
    case MoveLeft       => "<-"
    case MoveRight      => "->"
    case MoveUp         => "_/"
    case Field(f)       => "--(" + f + ")"
    case DownField(f)   => "--\\(" + f + ")"
    case DownArray      => "\\\\"
    case DownN(n)       => "=\\(" + n + ")"
    case DeleteGoParent => "!_/"
  }

  implicit final val eqCursorOp: Eq[CursorOp] = Eq.fromUniversalEquals

  val eqCursorOpList: Eq[List[CursorOp]] = cats.instances.list.catsKernelStdEqForList[CursorOp]

  /**
   * Represents JavaScript-style selections into a JSON structure.
   */
  private[this] sealed trait Selection
  private[this] case class SelectField(field: String) extends Selection
  private[this] case class SelectIndex(index: Int) extends Selection
  @deprecated
  private[this] case class Op(op: CursorOp) extends Selection

  /** Shows history as JS style selections, i.e. ".foo.bar[3]" */
  def opsToPath(history: List[CursorOp]): String = {
    @tailrec
    def getSelections(ops: List[CursorOp], acc: List[Selection]): List[Selection] =
      (ops, acc) match {
        case (Nil, acc)                                              => acc
        case (DownField(k) :: tail, acc)                             => getSelections(tail, SelectField(k) :: acc)
        case (DownN(n) :: tail, acc)                                 => getSelections(tail, SelectIndex(n) :: acc)
        case (DownArray :: DeleteGoParent :: DownArray :: tail, acc) => getSelections(tail, SelectIndex(1) :: acc)
        case (DownArray :: tail, acc)                                => getSelections(tail, SelectIndex(0) :: acc)
        case (Field(f) :: tail, SelectField(_) :: accTail)           => getSelections(tail, SelectField(f) :: accTail)
        case (MoveRight :: tail, SelectIndex(i) :: accTail) => getSelections(tail, SelectIndex(i + 1) :: accTail)
        case (MoveLeft :: tail, SelectIndex(i) :: accTail)  => getSelections(tail, SelectIndex(i - 1) :: accTail)
        case (MoveUp :: tail, _ :: tailAcc)                 => getSelections(tail, tailAcc)
        case (_, acc)                                       => acc // This should never happen
      }

    def showSelections(selections: List[Selection]): String = {
      selections.map {
        case SelectField(f) => s".$f"
        case SelectIndex(i) => s"[$i]"
        case Op(_)          => "" // This never happens
      }.mkString
    }

    showSelections(getSelections(history.reverse, Nil).reverse)
  }
}
