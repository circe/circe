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

import scala.compiletime.{ codeOf, constValue, erasedValue, error, summonFrom, summonInline }
import scala.deriving.Mirror
import scala.collection.immutable.SortedSet
import io.circe.{ Codec, Decoder, Encoder, KeyDecoder, KeyEncoder }
import cats.data.{ Chain, NonEmptyChain, NonEmptyList, NonEmptyMap, NonEmptySet, NonEmptyVector }
import cats.kernel.Order

private[circe] inline final def summonLabels[T <: Tuple]: List[String] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts)  => constValue[t].asInstanceOf[String] :: summonLabels[ts]

@deprecated("Use summonEncoders(derivingForSum: Boolean) instead", "0.14.7")
private[circe] inline final def summonEncoders[T <: Tuple](using Configuration): List[Encoder[_]] =
  summonEncoders(false)

private[circe] inline final def summonEncoders[T <: Tuple](inline derivingForSum: Boolean)(using
  Configuration
): List[Encoder[_]] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts)  => summonEncoder[t](derivingForSum) :: summonEncoders[ts](derivingForSum)

@deprecated("Use summonEncoder(derivingForSum: Boolean) instead", "0.14.7")
private[circe] inline final def summonEncoder[A](using Configuration): Encoder[A] =
  summonEncoder(false)

private[circe] inline final def summonEncoder[A](inline derivingForSum: Boolean)(using Configuration): Encoder[A] =
  summonFrom {
    case encodeA: Encoder[A] => encodeA
    case m: Mirror.Of[A] =>
      inline if (derivingForSum) ConfiguredEncoder.derived[A]
      else error("Failed to find an instance of Encoder[" + typeName[A] + "]")
  }

@deprecated("Use summonDecoders(derivingForSum: Boolean) instead", "0.14.7")
private[circe] inline final def summonDecoders[T <: Tuple](using Configuration): List[Decoder[_]] =
  summonDecoders(false)

private[circe] inline final def summonDecoders[T <: Tuple](inline derivingForSum: Boolean)(using
  Configuration
): List[Decoder[_]] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts)  => summonDecoder[t](derivingForSum) :: summonDecoders[ts](derivingForSum)

@deprecated("Use summonDecoder(derivingForSum: Boolean) instead", "0.14.7")
private[circe] inline final def summonDecoder[A](using Configuration): Decoder[A] =
  summonDecoder(false)

private[circe] inline final def summonDecoder[A](inline derivingForSum: Boolean)(using Configuration): Decoder[A] =
  summonFrom {
    case decodeA: Decoder[A] => decodeA
    case m: Mirror.Of[A] =>
      inline if (derivingForSum) ConfiguredDecoder.derived[A]
      else error("Failed to find an instance of Decoder[" + typeName[A] + "]")
  }

private[circe] inline def summonSingletonCases[T <: Tuple, A](inline typeName: Any): List[A] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (h *: t) =>
      inline summonInline[Mirror.Of[h]] match
        case m: Mirror.Singleton => m.fromProduct(EmptyTuple).asInstanceOf[A] :: summonSingletonCases[t, A](typeName)
        case m: Mirror =>
          error("Enum " + codeOf(typeName) + " contains non singleton case " + codeOf(constValue[m.MirroredLabel]))
