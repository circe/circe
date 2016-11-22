package io.circe.jackson

import cats.data.ValidatedNel
import io.circe.{ Decoder, Error, Parser, ParsingFailure }
import io.circe.ast.Json
import java.io.File
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

  final def decodeByteArray[A: Decoder](bytes: Array[Byte]): Either[Error, A] =
    finishDecode[A](parseByteArray(bytes))

  final def decodeByteArrayAccumulating[A: Decoder](bytes: Array[Byte]): ValidatedNel[Error, A] =
    finishDecodeAccumulating[A](parseByteArray(bytes))

  final def decodeFile[A: Decoder](file: File): Either[Error, A] =
    finishDecode[A](parseFile(file))

  final def decodeFileAccumulating[A: Decoder](file: File): ValidatedNel[Error, A] =
    finishDecodeAccumulating[A](parseFile(file))
}
