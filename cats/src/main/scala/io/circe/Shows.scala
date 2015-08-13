package io.circe

import cats.Show
import io.circe.Context.{ObjectContext, ArrayContext}
import io.circe.CursorOp.{El, Reattempt}
import io.circe.CursorOpElement._

trait Shows {
  implicit val showContext: Show[Context] = Show.show {
    case ArrayContext(_, i) => s"[$i]"
    case ObjectContext(_, f) => s"{$f}"
  }

  implicit val showCursor: Show[Cursor] = Show.show { c =>
    val sc = Show[Context]
    s"${ c.context.map(e => sc.show(e)).mkString(", ") } ==> ${ Show[Json].show(c.focus) }"
  }

  implicit val showCursorOp: Show[CursorOp] = Show.show {
    case Reattempt => ".?."
    case El(o, s) =>
      val shownOp = Show[CursorOpElement].show(o)
      if (s) shownOp else s"*.$shownOp"
  }

  implicit val showCursorOpElement: Show[CursorOpElement] = Show.show {
    case CursorOpLeft => "<-"
    case CursorOpRight => "->"
    case CursorOpFirst => "|<-"
    case CursorOpLast => "->|"
    case CursorOpUp => "_/"
    case CursorOpLeftN(n) => "-<-:(" + n + ")"
    case CursorOpRightN(n) => ":->-(" + n + ")"
    case CursorOpLeftAt(_) => "?<-:"
    case CursorOpRightAt(_) => ":->?"
    case CursorOpFind(_) => "find"
    case CursorOpField(f) => "--(" + f + ")"
    case CursorOpDownField(f) => "--\\(" + f + ")"
    case CursorOpDownArray => "\\\\"
    case CursorOpDownAt(_) => "-\\"
    case CursorOpDownN(n) => "=\\(" + n + ")"
    case CursorOpDeleteGoParent => "!_/"
    case CursorOpDeleteGoLeft => "<-!"
    case CursorOpDeleteGoRight => "!->"
    case CursorOpDeleteGoFirst => "|<-!"
    case CursorOpDeleteGoLast => "!->|"
    case CursorOpDeleteGoField(f) => "!--(" + f + ")"
    case CursorOpDeleteLefts => "!<"
    case CursorOpDeleteRights => ">!"
    case CursorOpSetLefts(_) => "!<.."
    case CursorOpSetRights(_) => "..>!"
  }

  implicit val showJson: Show[Json] = Show.fromToString[Json]
  implicit val showJsonObject: Show[JsonObject] = Show.fromToString
}
