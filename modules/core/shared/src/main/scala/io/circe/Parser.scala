package io.circe

import cats.data.{ NonEmptyList, Validated, ValidatedNel }
import java.io.Serializable

trait Parser extends Serializable {
  def parse(input: String): Either[ParsingFailure, Json]

  protected[this] final def finishDecode[A](input: Either[ParsingFailure, Json])(
    implicit
    decoder: Decoder[A]
  ): Either[Error, A] = input match {
    case Right(json) => decoder.decodeJson(json)
    case l @ Left(_) => l.asInstanceOf[Either[Error, A]]
  }

  protected[this] final def finishDecodeAccumulating[A](input: Either[ParsingFailure, Json])(
    implicit
    decoder: Decoder[A]
  ): ValidatedNel[Error, A] = input match {
    case Right(json) =>
      decoder.decodeAccumulating(json.hcursor).leftMap {
        case NonEmptyList(h, t) => NonEmptyList(h, t)
      }
    case Left(error) => Validated.invalidNel(error)
  }

  final def decode[A: Decoder](input: String): Either[Error, A] =
    finishDecode(parse(input))

  final def decodeAccumulating[A: Decoder](input: String): ValidatedNel[Error, A] =
    finishDecodeAccumulating(parse(input))
}
