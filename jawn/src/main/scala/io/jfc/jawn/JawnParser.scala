package io.jfc.jawn

import cats.data.Xor
import io.jfc.{ Json, ParseFailure, Parser }
import java.io.File
import java.nio.ByteBuffer
import scala.util.Try

class JawnParser extends Parser {
  private[this] def fromTry(t: Try[Json]): Xor[ParseFailure, Json] =
    Xor.fromTry(t).leftMap(error => ParseFailure(error.getMessage, error))

  def parse(input: String): Xor[ParseFailure, Json] =
    fromTry(JfcSupportParser.parseFromString(input))

  def parseFile(file: File): Xor[ParseFailure, Json] =
    fromTry(JfcSupportParser.parseFromFile(file))

  def parseByteBuffer(buffer: ByteBuffer): Xor[ParseFailure, Json] =
    fromTry(JfcSupportParser.parseFromByteBuffer(buffer))
}
