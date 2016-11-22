package io.circe

import cats.{ Eq, Show }
import io.circe.ast.Json

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

final object CursorOp {
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

  final case object MoveLeft extends UnconstrainedOp
  final case object MoveRight extends UnconstrainedOp
  final case object MoveFirst extends UnconstrainedOp
  final case object MoveLast extends UnconstrainedOp
  final case object MoveUp extends UnconstrainedOp
  final case class LeftN(n: Int) extends UnconstrainedOp
  final case class RightN(n: Int) extends UnconstrainedOp
  final case class LeftAt(p: Json => Boolean) extends UnconstrainedOp
  final case class RightAt(p: Json => Boolean) extends UnconstrainedOp
  final case class Find(p: Json => Boolean) extends UnconstrainedOp
  final case class Field(k: String) extends UnconstrainedOp
  final case class DownField(k: String) extends ObjectOp
  final case object DownArray extends ArrayOp
  final case class DownAt(p: Json => Boolean) extends ArrayOp
  final case class DownN(n: Int) extends ArrayOp
  final case object DeleteGoParent extends UnconstrainedOp
  final case object DeleteGoLeft extends UnconstrainedOp
  final case object DeleteGoRight extends UnconstrainedOp
  final case object DeleteGoFirst extends UnconstrainedOp
  final case object DeleteGoLast extends UnconstrainedOp
  final case class DeleteGoField(k: String) extends UnconstrainedOp
  final case object DeleteLefts extends UnconstrainedOp
  final case object DeleteRights extends UnconstrainedOp
  final case class SetLefts(js: List[Json]) extends UnconstrainedOp
  final case class SetRights(js: List[Json]) extends UnconstrainedOp

  implicit final val showCursorOp: Show[CursorOp] = Show.show {
    case MoveLeft => "<-"
    case MoveRight => "->"
    case MoveFirst => "|<-"
    case MoveLast => "->|"
    case MoveUp => "_/"
    case LeftN(n) => "-<-:(" + n + ")"
    case RightN(n) => ":->-(" + n + ")"
    case LeftAt(_) => "?<-:"
    case RightAt(_) => ":->?"
    case Find(_) => "find"
    case Field(f) => "--(" + f + ")"
    case DownField(f) => "--\\(" + f + ")"
    case DownArray => "\\\\"
    case DownAt(_) => "-\\"
    case DownN(n) => "=\\(" + n + ")"
    case DeleteGoParent => "!_/"
    case DeleteGoLeft => "<-!"
    case DeleteGoRight => "!->"
    case DeleteGoFirst => "|<-!"
    case DeleteGoLast => "!->|"
    case DeleteGoField(f) => "!--(" + f + ")"
    case DeleteLefts => "!<"
    case DeleteRights => ">!"
    case SetLefts(_) => "!<.."
    case SetRights(_) => "..>!"
  }

  implicit final val eqCursorOp: Eq[CursorOp] = Eq.fromUniversalEquals
}
