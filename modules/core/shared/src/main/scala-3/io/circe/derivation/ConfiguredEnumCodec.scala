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

package io.circe.derivation

import scala.deriving.Mirror
import io.circe.{ Codec, Decoder, HCursor, Json }

trait ConfiguredEnumCodec[A] extends Codec[A]
object ConfiguredEnumCodec:
  inline final def derived[A](using conf: Configuration)(using Mirror.SumOf[A]): ConfiguredEnumCodec[A] =
    val decoder = ConfiguredEnumDecoder.derived[A]
    val encoder = ConfiguredEnumEncoder.derived[A]
    new ConfiguredEnumCodec[A]:
      override def apply(c: HCursor): Decoder.Result[A] = decoder(c)
      override def apply(a: A): Json = encoder(a)

  inline final def derive[R: Mirror.SumOf](
    transformConstructorNames: String => String = Configuration.default.transformConstructorNames
  ): Codec[R] = Codec.from(
    ConfiguredEnumDecoder.derive(transformConstructorNames),
    ConfiguredEnumEncoder.derive(transformConstructorNames)
  )
