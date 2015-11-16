package io.circe

import cats.data.Xor
import io.circe.scalajs._
import scala.scalajs.js.{
  JSON,
  SyntaxError,
  JavaScriptException
}

package object parse extends Parser {
  def parse(input: String): Xor[ParsingFailure, Json] = try {
    Xor.right(convertJSAnyToJson(JSON.parse(input)))
  } catch {
    case exception @ JavaScriptException(error: SyntaxError) =>
      Xor.left(ParsingFailure(error.message, exception))
  }

}
