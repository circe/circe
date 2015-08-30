package io.circe

import cats.data.Xor

trait Parser {
  def parse(input: String): Xor[ParsingFailure, Json]

  def decode[A](input: String)(implicit d: Decoder[A]): Xor[Error, A] =
    parse(input).flatMap { json => d(Cursor(json).hcursor) }
}
