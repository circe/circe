package io.circe.jawn

import io.circe.{ Json, ParsingFailure, Parser }
import java.io.File
import java.nio.ByteBuffer
import scala.util.{Failure, Success, Try}

class JawnParser extends Parser {
  private[this] def fromTry(t: Try[Json]): Either[ParsingFailure, Json] =
    t match {
      case Success(s) => Right(s)
      case Failure(error) => Left(ParsingFailure(error.getMessage, error))
    }

  def parse(input: String): Either[ParsingFailure, Json] =
    fromTry(CirceSupportParser.parseFromString(input))

  def parseFile(file: File): Either[ParsingFailure, Json] =
    fromTry(CirceSupportParser.parseFromFile(file))

  def parseByteBuffer(buffer: ByteBuffer): Either[ParsingFailure, Json] =
    fromTry(CirceSupportParser.parseFromByteBuffer(buffer))
}
