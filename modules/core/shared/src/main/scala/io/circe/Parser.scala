package io.circe

import cats.data.{ NonEmptyList, Validated, ValidatedNel, Xor }

trait Parser extends Serializable {
  def parse(input: String): Xor[ParsingFailure, Json]

  final def decode[A](input: String)(implicit decoder: Decoder[A]): Xor[Error, A] =
    parse(input).flatMap(decoder.decodeJson)

  final def decodeAccumulating[A](input: String)(implicit decoder: Decoder[A]): ValidatedNel[Error, A] =
    parse(input) match {
      case Xor.Right(json) => decoder.accumulating(json.hcursor).leftMap {
        case NonEmptyList(h, t) => NonEmptyList(h, t)
      }
      case Xor.Left(error) => Validated.invalidNel(error)
    }
}
