package io.circe.jawn

import io.circe.{ Json, Parser, ParsingFailure }
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
}
