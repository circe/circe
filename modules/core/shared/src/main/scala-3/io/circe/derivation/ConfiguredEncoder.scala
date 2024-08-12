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
import scala.quoted.*
import io.circe.{ Encoder, Json, JsonObject }

trait ConfiguredEncoder[A](using conf: Configuration) extends Encoder.AsObject[A]:
  lazy val elemLabels: List[String]
  lazy val elemEncoders: List[Encoder[?]]

  final def encodeElemAt(index: Int, elem: Any, transformName: String => String): (String, Json) = {
    (transformName(elemLabels(index)), elemEncoders(index).asInstanceOf[Encoder[Any]].apply(elem))
  }

  final def encodeProduct(a: A): JsonObject =
    val product = a.asInstanceOf[Product]
    val iterable = Iterable.tabulate(product.productArity) { index =>
      encodeElemAt(index, product.productElement(index), conf.transformMemberNames)
    }
    JsonObject.fromIterable(iterable)

  final def encodeSum(index: Int, a: A): JsonObject =
    val (constructorName, json) = encodeElemAt(index, a, conf.transformConstructorNames)
    val jo = json.asObject.getOrElse(JsonObject.empty)
    val elemIsSum = elemEncoders(index) match {
      case ce: ConfiguredEncoder[?] with SumOrProduct => ce.isSum
      case _                                          => conf.discriminator.exists(jo.contains)
    }
    if (elemIsSum)
      jo
    else
      // only add discriminator if elem is a Product
      conf.discriminator match
        case Some(discriminator) =>
          jo.add(discriminator, Json.fromString(constructorName))

        case None =>
          JsonObject.singleton(constructorName, json)

object ConfiguredEncoder:
  @deprecated("Use ofProduct and ofSum", "0.14.10")
  private[derivation] def inline$of[A](encoders: => List[Encoder[?]], labels: List[String])(using
    conf: Configuration,
    mirror: Mirror.Of[A]
  ): ConfiguredEncoder[A] = mirror match
    case _: Mirror.ProductOf[A] =>
      new ConfiguredEncoder[A] with SumOrProduct:
        lazy val elemEncoders = encoders
        lazy val elemLabels = labels
        def isSum = false
        def encodeObject(a: A) = encodeProduct(a)
    case mirror: Mirror.SumOf[A] =>
      new ConfiguredEncoder[A] with SumOrProduct:
        lazy val elemEncoders = encoders
        lazy val elemLabels = labels
        def isSum = true
        def encodeObject(a: A) = encodeSum(mirror.ordinal(a), a)

  private def ofProduct[A](encoders: => List[Encoder[?]], labels: List[String])(using
    conf: Configuration
  ): ConfiguredEncoder[A] =
    new ConfiguredEncoder[A] with SumOrProduct:
      lazy val elemEncoders = encoders
      lazy val elemLabels = labels
      def isSum = false
      def encodeObject(a: A) = encodeProduct(a)

  private def ofSum[A](
    encoders: => List[Encoder[?]],
    labels: List[String],
    ordinal: => A => Int
  )(using conf: Configuration): ConfiguredEncoder[A] =
    new ConfiguredEncoder[A] with SumOrProduct:
      private lazy val o: A => Int = ordinal

      lazy val elemEncoders = encoders
      lazy val elemLabels = labels
      def isSum = true
      def encodeObject(a: A) = encodeSum(o(a), a)

  private def derivedImpl[A: Type](
    conf: Expr[Configuration],
    mirror: Expr[Mirror.Of[A]]
  )(using q: Quotes): Expr[ConfiguredEncoder[A]] = {
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
          ConfiguredEncoder.ofProduct[A](
            summonEncoders[et & Tuple](false)(using $conf),
            summonLabels[el & Tuple]
          )(using $conf)
        }

      case '{
            $m: Mirror.SumOf[A] {
              type MirroredLabel = l
              type MirroredElemLabels = el
              type MirroredElemTypes = et
            }
          } =>
        '{
          ConfiguredEncoder.ofSum[A](
            summonEncoders[et & Tuple](true)(using $conf),
            summonLabels[el & Tuple],
            $m.ordinal
          )(using $conf)
        }
    }
  }

  inline final def derived[A](using conf: Configuration, inline mirror: Mirror.Of[A]): ConfiguredEncoder[A] =
    ${ derivedImpl[A]('conf, 'mirror) }

  inline final def derive[A: Mirror.Of](
    transformMemberNames: String => String = Configuration.default.transformMemberNames,
    transformConstructorNames: String => String = Configuration.default.transformConstructorNames,
    discriminator: Option[String] = Configuration.default.discriminator
  ): ConfiguredEncoder[A] =
    derived[A](using Configuration(transformMemberNames, transformConstructorNames, useDefaults = false, discriminator))
