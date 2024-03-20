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
import scala.compiletime.constValue
import io.circe.{ Codec, Decoder, Encoder, HCursor, JsonObject }

trait ConfiguredCodec[A] extends Codec.AsObject[A], ConfiguredDecoder[A], ConfiguredEncoder[A]
object ConfiguredCodec:

  inline final def derived[A](using conf: Configuration)(using
    mirror: Mirror.Of[A]
  ): ConfiguredCodec[A] =
    new ConfiguredCodec[A] with SumOrProduct:
      val name = constValue[mirror.MirroredLabel]
      lazy val elemLabels: List[String] = summonLabels[mirror.MirroredElemLabels]
      lazy val elemEncoders: List[Encoder[?]] = summonEncoders[mirror.MirroredElemTypes]
      lazy val elemDecoders: List[Decoder[?]] = summonDecoders[mirror.MirroredElemTypes]
      lazy val elemDefaults: Default[A] = Predef.summon[Default[A]]
      lazy val isSum: Boolean =
        inline mirror match
          case _: Mirror.ProductOf[A] => false
          case _: Mirror.SumOf[A]     => true

      final def encodeObject(a: A): JsonObject =
        inline mirror match
          case _: Mirror.ProductOf[A] => encodeProduct(a)
          case sum: Mirror.SumOf[A]   => encodeSum(sum.ordinal(a), a)

      final def apply(c: HCursor): Decoder.Result[A] =
        inline mirror match
          case product: Mirror.ProductOf[A] => decodeProduct(c, product.fromProduct)
          case _: Mirror.SumOf[A]           => decodeSum(c)

      final override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
        inline mirror match
          case product: Mirror.ProductOf[A] => decodeProductAccumulating(c, product.fromProduct)
          case _: Mirror.SumOf[A]           => decodeSumAccumulating(c)

  inline final def derive[A: Mirror.Of](
    transformMemberNames: String => String = Configuration.default.transformMemberNames,
    transformConstructorNames: String => String = Configuration.default.transformConstructorNames,
    useDefaults: Boolean = Configuration.default.useDefaults,
    discriminator: Option[String] = Configuration.default.discriminator,
    strictDecoding: Boolean = Configuration.default.strictDecoding,
    dropNoneValues: Boolean = false
  ): ConfiguredCodec[A] =
    derived[A](using
      Configuration(
        transformMemberNames,
        transformConstructorNames,
        useDefaults,
        discriminator,
        strictDecoding,
        dropNoneValues
      )
    )
