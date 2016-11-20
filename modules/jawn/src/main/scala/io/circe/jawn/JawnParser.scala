package io.circe.jawn

import cats.data.ValidatedNel
import io.circe.{ Decoder, Error, Json, Parser, ParsingFailure }
import java.io.File
import java.nio.ByteBuffer
import scala.util.{ Failure, Success, Try }

class JawnParser extends Parser {
  private[this] final def fromTry(t: Try[Json]): Either[ParsingFailure, Json] = t match {
    case Success(json) => Right(json)
    case Failure(error) => Left(ParsingFailure(error.getMessage, error))
  }

  final def parse(input: String): Either[ParsingFailure, Json] =
    fromTry(CirceSupportParser.parseFromString(input))

  final def parseFile(file: File): Either[ParsingFailure, Json] =
    fromTry(CirceSupportParser.parseFromFile(file))

  final def parseByteBuffer(buffer: ByteBuffer): Either[ParsingFailure, Json] =
    fromTry(CirceSupportParser.parseFromByteBuffer(buffer))

  final def decodeByteBuffer[A: Decoder](buffer: ByteBuffer): Either[Error, A] =
    finishDecode[A](parseByteBuffer(buffer))

  final def decodeByteBufferAccumulating[A: Decoder](buffer: ByteBuffer): ValidatedNel[Error, A] =
    finishDecodeAccumulating[A](parseByteBuffer(buffer))

  final def decodeFile[A: Decoder](file: File): Either[Error, A] =
    finishDecode[A](parseFile(file))

  final def decodeFileAccumulating[A: Decoder](file: File): ValidatedNel[Error, A] =
    finishDecodeAccumulating[A](parseFile(file))
}
