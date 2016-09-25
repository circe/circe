package io.circe.jackson

import io.circe.{ Json, Parser, ParsingFailure }
import java.io.File
import scala.Predef.classOf
import scala.util.control.NonFatal

trait JacksonParser extends Parser { this: WithJacksonMapper =>
  final def parse(input: String): Either[ParsingFailure, Json] = try {
    Right(mapper.readValue(jsonStringParser(input), classOf[Json]))
  } catch {
    case NonFatal(error) => Left(ParsingFailure(error.getMessage, error))
  }

  final def parseFile(file: File): Either[ParsingFailure, Json] = try {
    Right(mapper.readValue(jsonFileParser(file), classOf[Json]))
  } catch {
    case NonFatal(error) => Left(ParsingFailure(error.getMessage, error))
  }

  final def parseByteArray(bytes: Array[Byte]): Either[ParsingFailure, Json] = try {
    Right(mapper.readValue(jsonBytesParser(bytes), classOf[Json]))
  } catch {
    case NonFatal(error) => Left(ParsingFailure(error.getMessage, error))
  }
}
