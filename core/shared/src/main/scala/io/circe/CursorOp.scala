package io.circe

import algebra.Eq
import cats.Show

sealed abstract class CursorOp extends Product with Serializable

final object CursorOp {
  final case object MoveLeft extends CursorOp
  final case object MoveRight extends CursorOp
  final case object MoveFirst extends CursorOp
  final case object MoveLast extends CursorOp
  final case object MoveUp extends CursorOp
  final case class LeftN(n: Int) extends CursorOp
  final case class RightN(n: Int) extends CursorOp
  final case class LeftAt(p: Json => Boolean) extends CursorOp
  final case class RightAt(p: Json => Boolean) extends CursorOp
  final case class Find(p: Json => Boolean) extends CursorOp
  final case class Field(k: String) extends CursorOp
  final case class DownField(k: String) extends CursorOp
  final case object DownArray extends CursorOp
  final case class DownAt(p: Json => Boolean) extends CursorOp
  final case class DownN(n: Int) extends CursorOp
  final case object DeleteGoParent extends CursorOp
  final case object DeleteGoLeft extends CursorOp
  final case object DeleteGoRight extends CursorOp
  final case object DeleteGoFirst extends CursorOp
  final case object DeleteGoLast extends CursorOp
  final case class DeleteGoField(k: String) extends CursorOp
  final case object DeleteLefts extends CursorOp
  final case object DeleteRights extends CursorOp
  final case class SetLefts(js: List[Json]) extends CursorOp
  final case class SetRights(js: List[Json]) extends CursorOp

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
