/*
 * Copyright 2023 circe
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

package io.circe

import cats.data.NonEmptyList
import cats.data.Validated
import cats.data.ValidatedNel

import java.io.Serializable

trait Parser extends Serializable {
  def parse(input: String): Either[ParsingFailure, Json]

  protected[this] final def finishDecode[A](input: Either[ParsingFailure, Json])(implicit
    decoder: Decoder[A]
  ): Either[Error, A] = input match {
    case Right(json) => decoder.decodeJson(json)
    case l @ Left(_) => l.asInstanceOf[Either[Error, A]]
  }

  protected[this] final def finishDecodeAccumulating[A](input: Either[ParsingFailure, Json])(implicit
    decoder: Decoder[A]
  ): ValidatedNel[Error, A] = input match {
    case Right(json) =>
      decoder.decodeAccumulating(json.hcursor).leftMap {
        case NonEmptyList(h, t) => NonEmptyList(h, t)
      }
    case Left(error) => Validated.invalidNel(error)
  }

  final def decode[A: Decoder](input: String): Either[Error, A] =
    finishDecode(parse(input))

  final def decodeAccumulating[A: Decoder](input: String): ValidatedNel[Error, A] =
    finishDecodeAccumulating(parse(input))
}
