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
import io.circe.{ Codec, Decoder, Encoder, HCursor }

trait ConfiguredEnumCodec[A] extends Codec[A]
object ConfiguredEnumCodec:
  private def of[A](decoder: Decoder[A], encoder: Encoder[A]): ConfiguredEnumCodec[A] =
    new ConfiguredEnumCodec[A]:
      def apply(c: HCursor) = decoder(c)
      def apply(a: A) = encoder(a)

  inline final def derived[A: Mirror.SumOf](using conf: Configuration): ConfiguredEnumCodec[A] =
    ConfiguredEnumCodec.of[A](ConfiguredEnumDecoder.derived[A], ConfiguredEnumEncoder.derived[A])

  inline final def derive[R: Mirror.SumOf](
    transformConstructorNames: String => String = Configuration.default.transformConstructorNames
  ): Codec[R] = Codec.from(
    ConfiguredEnumDecoder.derive(transformConstructorNames),
    ConfiguredEnumEncoder.derive(transformConstructorNames)
  )
