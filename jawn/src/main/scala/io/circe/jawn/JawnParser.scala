package io.circe.jawn

import cats.data.Xor
import io.circe.{ Json, Parser, ParsingFailure }
import java.io.File
import java.nio.ByteBuffer
import scala.util.Try

class JawnParser extends Parser {
  private[this] def fromTry(t: Try[Json]): Xor[ParsingFailure, Json] =
    Xor.fromTry(t).leftMap(error => ParsingFailure(error.getMessage, error))

  def parse(input: String): Xor[ParsingFailure, Json] =
    fromTry(CirceSupportParser.parseFromString(input))

  def parseFile(file: File): Xor[ParsingFailure, Json] =
    fromTry(CirceSupportParser.parseFromFile(file))

  def parseByteBuffer(buffer: ByteBuffer): Xor[ParsingFailure, Json] =
    fromTry(CirceSupportParser.parseFromByteBuffer(buffer))
}
