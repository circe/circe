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

import cats.data.Validated

/**
 * [[Decoder]] and [[Encoder]] instances for disjunction types with reasonable names for the sides.
 */
object disjunctionCodecs {
  private[this] final val leftKey: String = "Left"
  private[this] final val rightKey: String = "Right"
  private[this] final val failureKey: String = "Invalid"
  private[this] final val successKey: String = "Valid"

  implicit final def decoderEither[A, B](implicit
    da: Decoder[A],
    db: Decoder[B]
  ): Decoder[Either[A, B]] = Decoder.decodeEither(leftKey, rightKey)

  implicit final def decodeValidated[E, A](implicit
    de: Decoder[E],
    da: Decoder[A]
  ): Decoder[Validated[E, A]] = Decoder.decodeValidated(failureKey, successKey)

  implicit final def encodeEither[A, B](implicit
    ea: Encoder[A],
    eb: Encoder[B]
  ): Encoder.AsObject[Either[A, B]] =
    Encoder.encodeEither(leftKey, rightKey)

  implicit final def encodeValidated[E, A](implicit
    ee: Encoder[E],
    ea: Encoder[A]
  ): Encoder.AsObject[Validated[E, A]] =
    Encoder.encodeValidated(failureKey, successKey)
}
