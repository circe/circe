package io.circe

import cats.data.Xor
import scalajs.js.{
  Array => JsArray,
  Dictionary,
  JSON,
  JavaScriptException,
  Object => JsObject,
  SyntaxError
}

package object parse extends Parser {
  def parse(input: String): Xor[ParsingFailure, Json] = try {
    Xor.right(convertJson(JSON.parse(input)))
  } catch {
    case exception @ JavaScriptException(error: SyntaxError) =>
      Xor.left(ParsingFailure(error.message, exception))
  }

  private[this] def convertJson(j: Any): Json = j match {
    case s: String => Json.string(s)
    case n: Double => Json.numberOrNull(n)
    case true => Json.True
    case false => Json.False
    case null => Json.Empty
    case a: JsArray[_] => Json.fromValues(a.map(convertJson(_: Any)))
    case o: JsObject => Json.fromFields(
      o.asInstanceOf[Dictionary[_]].mapValues(convertJson).toSeq
    )
  }
}
