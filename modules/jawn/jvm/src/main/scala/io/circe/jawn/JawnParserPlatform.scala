package io.circe.jawn

import cats.data.ValidatedNel
import io.circe.Decoder
import io.circe.Error
import io.circe.Json
import io.circe.ParsingFailure

import java.io.File
import java.nio.channels.ReadableByteChannel
import java.nio.file.Files
import java.nio.file.Path

private[jawn] trait JawnParserPlatform { self: JawnParser =>
  final def parsePath(path: Path): Either[ParsingFailure, Json] =
    parseChannel(Files.newByteChannel(path))

  final def parseFile(file: File): Either[ParsingFailure, Json] =
    fromTry(supportParser.parseFromFile(file))

  final def parseChannel(ch: ReadableByteChannel): Either[ParsingFailure, Json] =
    fromTry(supportParser.parseFromChannel(ch))

  final def decodePath[A: Decoder](path: Path): Either[Error, A] =
    finishDecode[A](parsePath(path))

  final def decodePathAccumulating[A: Decoder](path: Path): ValidatedNel[Error, A] =
    finishDecodeAccumulating[A](parsePath(path))

  final def decodeFile[A: Decoder](file: File): Either[Error, A] =
    finishDecode[A](parseFile(file))

  final def decodeFileAccumulating[A: Decoder](file: File): ValidatedNel[Error, A] =
    finishDecodeAccumulating[A](parseFile(file))

  final def decodeChannel[A: Decoder](ch: ReadableByteChannel): Either[Error, A] =
    finishDecode[A](parseChannel(ch))

  final def decodeChannelAccumulating[A: Decoder](ch: ReadableByteChannel): ValidatedNel[Error, A] =
    finishDecodeAccumulating[A](parseChannel(ch))
}
