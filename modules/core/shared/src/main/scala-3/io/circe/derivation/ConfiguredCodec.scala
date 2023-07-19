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

package io.circe.derivation

import scala.deriving.Mirror
import io.circe.{ Codec, Decoder, HCursor, JsonObject }

trait ConfiguredCodec[A] extends Codec.AsObject[A]

object ConfiguredCodec:
  inline final def derive[A: Mirror.Of](
    transformMemberNames: String => String = Configuration.default.transformMemberNames,
    transformConstructorNames: String => String = Configuration.default.transformConstructorNames,
    useDefaults: Boolean = Configuration.default.useDefaults,
    discriminator: Option[String] = Configuration.default.discriminator,
    strictDecoding: Boolean = Configuration.default.strictDecoding
  ): ConfiguredCodec[A] =
    derived[A](using
      Configuration(transformMemberNames, transformConstructorNames, useDefaults, discriminator, strictDecoding)
    )
  inline final def derived[A](using Configuration)(using inline mirror: Mirror.Of[A]): ConfiguredCodec[A] =
    val encoder = ConfiguredEncoder.derived[A]
    val decoder = ConfiguredDecoder.derived[A]
    new ConfiguredCodec[A]:
      final def encodeObject(a: A): JsonObject = encoder.encodeObject(a)
      final def apply(c: HCursor): Decoder.Result[A] = decoder.apply(c)
