package io.circe

import java.io.Serializable
import cats.data.{ Validated, ValidatedNec }

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
  ): ValidatedNec[Error, A] = input match {
    case Right(json) => decoder.decodeAccumulating(json.hcursor)
    case Left(error) => Validated.invalidNec(error)
  }

  final def decode[A: Decoder](input: String): Either[Error, A] =
    finishDecode(parse(input))

  final def decodeAccumulating[A: Decoder](input: String): ValidatedNec[Error, A] =
    finishDecodeAccumulating(parse(input))
}
