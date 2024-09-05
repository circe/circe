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
import scala.compiletime.{ constValue, summonInline }
import scala.quoted.*
import io.circe.{ Codec, Decoder, Encoder, HCursor, JsonObject }

trait ConfiguredCodec[A] extends Codec.AsObject[A], ConfiguredDecoder[A], ConfiguredEncoder[A]
object ConfiguredCodec:
  @deprecated("Use ofProduct and ofSum", "0.14.10")
  private[derivation] def inline$of[A](
    nme: String,
    decoders: => List[Decoder[?]],
    encoders: => List[Encoder[?]],
    labels: List[String]
  )(using
    conf: Configuration,
    mirror: Mirror.Of[A],
    defaults: Default[A]
  ): ConfiguredCodec[A] = mirror match
    case mirror: Mirror.ProductOf[A] =>
      new ConfiguredCodec[A] with SumOrProduct:
        val name = nme
        lazy val elemDecoders = decoders
        lazy val elemEncoders = encoders
        lazy val elemLabels = labels
        lazy val elemDefaults = defaults
        def isSum = false
        def apply(c: HCursor) = decodeProduct(c, mirror.fromProduct)
        def encodeObject(a: A) = encodeProduct(a)
        override def decodeAccumulating(c: HCursor) = decodeProductAccumulating(c, mirror.fromProduct)
    case mirror: Mirror.SumOf[A] =>
      new ConfiguredCodec[A] with SumOrProduct:
        val name = nme
        lazy val elemDecoders = decoders
        lazy val elemEncoders = encoders
        lazy val elemLabels = labels
        lazy val elemDefaults = defaults
        def isSum = true
        def apply(c: HCursor) = decodeSum(c)
        def encodeObject(a: A) = encodeSum(mirror.ordinal(a), a)
        override def decodeAccumulating(c: HCursor) = decodeSumAccumulating(c)

  private def ofProduct[A](
    nme: String,
    decoders: => List[Decoder[?]],
    encoders: => List[Encoder[?]],
    labels: List[String],
    fromProduct: => Product => A
  )(using conf: Configuration, defaults: Default[A]): ConfiguredCodec[A] =
    new ConfiguredCodec[A] with SumOrProduct:
      private lazy val fp: Product => A = fromProduct

      val name = nme
      lazy val elemDecoders = decoders
      lazy val elemEncoders = encoders
      lazy val elemLabels = labels
      lazy val elemDefaults = defaults
      def isSum = false
      def apply(c: HCursor) = decodeProduct(c, fp)
      def encodeObject(a: A) = encodeProduct(a)
      override def decodeAccumulating(c: HCursor) = decodeProductAccumulating(c, fp)

  private def ofSum[A](
    nme: String,
    decoders: => List[Decoder[?]],
    encoders: => List[Encoder[?]],
    labels: List[String],
    ordinal: => A => Int
  )(using conf: Configuration, defaults: Default[A]): ConfiguredCodec[A] =
    new ConfiguredCodec[A] with SumOrProduct:
      private lazy val o: A => Int = ordinal

      val name = nme
      lazy val elemDecoders = decoders
      lazy val elemEncoders = encoders
      lazy val elemLabels = labels
      lazy val elemDefaults = defaults
      def isSum = true
      def apply(c: HCursor) = decodeSum(c)
      def encodeObject(a: A) = encodeSum(o(a), a)
      override def decodeAccumulating(c: HCursor) = decodeSumAccumulating(c)

  private def derivedImpl[A: Type](
    conf: Expr[Configuration],
    mirror: Expr[Mirror.Of[A]]
  )(using q: Quotes): Expr[ConfiguredCodec[A]] = {
    import q.reflect.*

    mirror match {
      case '{
            $m: Mirror.ProductOf[A] {
              type MirroredLabel = l
              type MirroredElemLabels = el
              type MirroredElemTypes = et
            }
          } =>
        '{
          ConfiguredCodec.ofProduct[A](
            constValue[l & String],
            summonDecoders[et & Tuple](false)(using $conf),
            summonEncoders[et & Tuple](false)(using $conf),
            summonLabels[el & Tuple],
            $m.fromProduct
          )(using $conf, summonInline[Default[A]])
        }

      case '{
            $m: Mirror.SumOf[A] {
              type MirroredLabel = l
              type MirroredElemLabels = el
              type MirroredElemTypes = et
            }
          } =>
        '{
          ConfiguredCodec.ofSum[A](
            constValue[l & String],
            summonDecoders[et & Tuple](true)(using $conf),
            summonEncoders[et & Tuple](true)(using $conf),
            summonLabels[el & Tuple],
            $m.ordinal
          )(using $conf, summonInline[Default[A]])
        }
    }
  }

  inline final def derived[A](using conf: Configuration, inline mirror: Mirror.Of[A]): ConfiguredCodec[A] =
    ${ derivedImpl[A]('conf, 'mirror) }

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
