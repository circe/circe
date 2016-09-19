package io.circe.jackson

import cats.data.Xor
import io.circe.{ Json, Parser, ParsingFailure }
import java.io.File
import scala.util.control.NonFatal

trait JacksonParser extends Parser { this: WithJacksonMapper =>
  final def parse(input: String): Xor[ParsingFailure, Json] = try {
    Xor.right(mapper.readValue(jsonStringParser(input), classOf[Json]))
  } catch {
    case NonFatal(error) => Xor.left(ParsingFailure(error.getMessage, error))
  }

  final def parseFile(file: File): Xor[ParsingFailure, Json] = try {
    Xor.right(mapper.readValue(jsonFileParser(file), classOf[Json]))
  } catch {
    case NonFatal(error) => Xor.left(ParsingFailure(error.getMessage, error))
  }

  final def parseByteArray(bytes: Array[Byte]): Xor[ParsingFailure, Json] = try {
    Xor.right(mapper.readValue(jsonBytesParser(bytes), classOf[Json]))
  } catch {
    case NonFatal(error) => Xor.left(ParsingFailure(error.getMessage, error))
  }
}
