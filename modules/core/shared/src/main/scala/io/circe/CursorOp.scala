/*
 * Copyright 2023 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe

import cats.Eq
import cats.Show

import java.io.Serializable

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
