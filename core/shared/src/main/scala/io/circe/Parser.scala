package io.circe

import cats.data.{ NonEmptyList, Validated, ValidatedNel }

trait Parser extends Serializable {
  def parse(input: String): Either[ParsingFailure, Json]

  final def decode[A](input: String)(implicit decoder: Decoder[A]): Either[Error, A] =
    parse(input) match {
      case Right(json) => decoder.decodeJson(json)
      case l @ Left(_) => l.asInstanceOf[Either[Error, A]]
    }

  final def decodeAccumulating[A](input: String)(implicit decoder: Decoder[A]): ValidatedNel[Error, A] =
    parse(input) match {
      case Right(json) => decoder.accumulating(json.hcursor).leftMap {
        case NonEmptyList(h, t) => NonEmptyList(h, t)
      }
      case Left(error) => Validated.invalidNel(error)
    }
}
