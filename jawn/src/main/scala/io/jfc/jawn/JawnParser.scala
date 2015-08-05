package io.jfc.jawn

import cats.data.Xor
import io.jfc.{ Json, ParsingFailure, Parser }
import java.io.File
import java.nio.ByteBuffer
import scala.util.Try

class JawnParser extends Parser {
  private[this] def fromTry(t: Try[Json]): Xor[ParsingFailure, Json] =
    Xor.fromTry(t).leftMap(error => ParsingFailure(error.getMessage, error))

  def parse(input: String): Xor[ParsingFailure, Json] =
    fromTry(JfcSupportParser.parseFromString(input))

  def parseFile(file: File): Xor[ParsingFailure, Json] =
    fromTry(JfcSupportParser.parseFromFile(file))

  def parseByteBuffer(buffer: ByteBuffer): Xor[ParsingFailure, Json] =
    fromTry(JfcSupportParser.parseFromByteBuffer(buffer))
}
