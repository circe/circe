package io.circe

import algebra.Eq
import cats.Show

sealed abstract class CursorOp extends Product with Serializable

object CursorOp {
  case object MoveLeft extends CursorOp
  case object MoveRight extends CursorOp
  case object MoveFirst extends CursorOp
  case object MoveLast extends CursorOp
  case object MoveUp extends CursorOp
  case class LeftN(n: Int) extends CursorOp
  case class RightN(n: Int) extends CursorOp
  case class LeftAt(p: Json => Boolean) extends CursorOp
  case class RightAt(p: Json => Boolean) extends CursorOp
  case class Find(p: Json => Boolean) extends CursorOp
  case class Field(k: String) extends CursorOp
  case class DownField(k: String) extends CursorOp
  case object DownArray extends CursorOp
  case class DownAt(p: Json => Boolean) extends CursorOp
  case class DownN(n: Int) extends CursorOp
  case object DeleteGoParent extends CursorOp
  case object DeleteGoLeft extends CursorOp
  case object DeleteGoRight extends CursorOp
  case object DeleteGoFirst extends CursorOp
  case object DeleteGoLast extends CursorOp
  case class DeleteGoField(k: String) extends CursorOp
  case object DeleteLefts extends CursorOp
  case object DeleteRights extends CursorOp
  case class SetLefts(js: List[Json]) extends CursorOp
  case class SetRights(js: List[Json]) extends CursorOp

  implicit val showCursorOp: Show[CursorOp] = Show.show {
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

  implicit val eqCursorOp: Eq[CursorOp] = Eq.fromUniversalEquals
}
