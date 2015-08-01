package io.jfc

import cats.data.Xor

trait Parser {
  def parse(input: String): Xor[ParseFailure, Json]
  def decode[A](input: String)(implicit d: Decode[A]): Xor[Error, A] =
    parse(input).flatMap { json => d(Cursor(json).hcursor) }
}
