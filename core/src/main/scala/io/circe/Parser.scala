package io.circe

trait Parser {
  def parse(input: String): Either[ParsingFailure, Json]
  def decode[A](input: String)(implicit d: Decoder[A]): Either[Error, A] =
    parse(input).right.flatMap { json => d(Cursor(json).hcursor) }
}
