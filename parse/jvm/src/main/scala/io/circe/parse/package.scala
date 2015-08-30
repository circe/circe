package io.circe

import cats.data.Xor
import io.circe.jawn.JawnParser

package object parse extends Parser {
  private[this] val parser = new JawnParser

  def parse(input: String): Xor[ParsingFailure, Json] = parser.parse(input)
}
