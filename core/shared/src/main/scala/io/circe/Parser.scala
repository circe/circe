package io.circe

import cats.data.Xor

trait Parser extends Serializable {
  def parse(input: String): Xor[ParsingFailure, Json]

  final def decode[A](input: String)(implicit d: Decoder[A]): Xor[Error, A] =
    parse(input).flatMap(d.decodeJson)
}
