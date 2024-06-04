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

import scala.compiletime.constValue
import io.circe.{ Codec, Decoder, Encoder, HCursor }

trait ConfiguredCodec[A] extends Codec.AsObject[A], ConfiguredDecoder[A], ConfiguredEncoder[A]
object ConfiguredCodec:
  // It's important that all of these parameters are by-name, see https://github.com/circe/circe/pull/2278
  private def of[A](
    nme: => String,
    decoders: => List[Decoder[?]],
    encoders: => List[Encoder[?]],
    labels: => List[String]
  )(using
    conf: Configuration,
    mirror: LazyMirror[A]
  ): ConfiguredCodec[A] = mirror match
    case mirror: LazyMirror.Product[A] =>
      new ConfiguredCodec[A] with SumOrProduct:
        val name = nme
        lazy val elemDecoders = decoders
        lazy val elemEncoders = encoders
        lazy val elemLabels = labels
        lazy val elemDefaults = mirror.default
        def isSum = false
        def apply(c: HCursor) = decodeProduct(c, mirror.fromProduct)
        def encodeObject(a: A) = encodeProduct(a)
        override def decodeAccumulating(c: HCursor) = decodeProductAccumulating(c, mirror.fromProduct)
    case mirror: LazyMirror.Sum[A] =>
      new ConfiguredCodec[A] with SumOrProduct:
        val name = nme
        lazy val elemDecoders = decoders
        lazy val elemEncoders = encoders
        lazy val elemLabels = labels
        lazy val elemDefaults = mirror.default
        def isSum = true
        def apply(c: HCursor) = decodeSum(c)
        def encodeObject(a: A) = encodeSum(mirror.ordinal(a), a)
        override def decodeAccumulating(c: HCursor) = decodeSumAccumulating(c)

  inline final def derived[A](using
    conf: Configuration,
    mirror: LazyMirror[A],
    decoders: Decoders[A],
    encoders: Encoders[A]
  ): ConfiguredCodec[A] =
    ConfiguredCodec.of(mirror.mirroredLabel, decoders.decoders, encoders.encoders, mirror.mirroredElemLabels)

  inline final def derive[A: LazyMirror: Decoders: Encoders](
    transformMemberNames: String => String = Configuration.default.transformMemberNames,
    transformConstructorNames: String => String = Configuration.default.transformConstructorNames,
    useDefaults: Boolean = Configuration.default.useDefaults,
    discriminator: Option[String] = Configuration.default.discriminator,
    strictDecoding: Boolean = Configuration.default.strictDecoding
  ): ConfiguredCodec[A] =
    derived[A](using
      Configuration(transformMemberNames, transformConstructorNames, useDefaults, discriminator, strictDecoding)
    )
