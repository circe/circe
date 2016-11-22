package io.circe

import io.circe.ast.Json
import io.circe.jawn.JawnParser

package object parser extends Parser {
  private[this] val parser = new JawnParser

  def parse(input: String): Either[ParsingFailure, Json] = parser.parse(input)
}
