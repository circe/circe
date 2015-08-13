package io.circe

import cats.Show
import io.circe.Context.{ObjectContext, ArrayContext}

trait Shows {
  implicit val showContext: Show[Context] = Show.show {
    case ArrayContext(_, i) => s"[$i]"
    case ObjectContext(_, f) => s"{$f}"
  }

  implicit val showCursor: Show[Cursor] = Show.show { c =>
    val sc = Show[Context]
    s"${ c.context.map(e => sc.show(e)).mkString(", ") } ==> ${ Show[Json].show(c.focus) }"
  }

  implicit val showJson: Show[Json] = Show.fromToString[Json]
  implicit val showJsonObject: Show[JsonObject] = Show.fromToString
}
