package io.circe

import cats.data.Xor
import io.circe.scalajs.convertJsToJson
import scala.scalajs.js.JSON

package object parser extends Parser {
  def parse(input: String): Xor[ParsingFailure, Json] =
    Xor.catchNonFatal(JSON.parse(input)).flatMap(convertJsToJson).leftMap(exception =>
      ParsingFailure(exception.getMessage, exception)
    )
}
