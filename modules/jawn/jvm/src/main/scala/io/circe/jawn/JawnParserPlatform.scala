/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
