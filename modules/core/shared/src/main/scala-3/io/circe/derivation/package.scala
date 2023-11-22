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

import scala.compiletime.{ codeOf, constValue, erasedValue, error, summonFrom, summonInline }
import scala.deriving.Mirror
import io.circe.{ Codec, Decoder, Encoder }

private[circe] inline final def summonLabels[T <: Tuple]: List[String] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts)  => constValue[t].asInstanceOf[String] :: summonLabels[ts]

private[circe] inline final def summonEncoders[T <: Tuple](
  inline autoRecurse: Boolean,
  inline derivingForSum: Boolean
)(using Configuration): List[Encoder[_]] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts) =>
      summonEncoder[t](autoRecurse, derivingForSum) :: summonEncoders[ts](autoRecurse, derivingForSum)

@deprecated("Use summonEncoders(autoRecurse: Boolean) instead", "0.14.6")
private[circe] inline final def summonEncoders[T <: Tuple](using Configuration): List[Encoder[_]] =
  summonEncoders(true, false)

private[circe] inline final def summonEncoderAutoRecurse[A](using Configuration): Encoder[A] =
  summonFrom {
    case encodeA: Encoder[A] => encodeA
    case _: Mirror.Of[A]     => ConfiguredEncoder.derived[A]
  }

private[circe] inline final def summonEncoderNoAutoRecurse[A](
  inline derivingForSum: Boolean
)(using conf: Configuration): Encoder[A] =
  summonFrom {
    case encodeA: Encoder[A] => encodeA
    case m: Mirror.Of[A] =>
      inline if (derivingForSum) ConfiguredEncoder.derivedNoAutoRecursion[A]
      else error("Failed to find an instance of Encoder[" + constValue[m.MirroredLabel] + "]")
  }

private[circe] inline final def summonEncoder[A](
  inline autoRecurse: Boolean,
  inline derivingForSum: Boolean
)(using Configuration): Encoder[A] =
  inline if (autoRecurse) summonEncoderAutoRecurse[A] else summonEncoderNoAutoRecurse[A](derivingForSum)

@deprecated("Use summonEncoder(autoRecurse: Boolean) instead", "0.14.6")
private[circe] inline final def summonEncoder[A](using Configuration): Encoder[A] = summonEncoder(true, false)

private[circe] inline final def summonDecoders[T <: Tuple](
  inline autoRecurse: Boolean,
  inline derivingForSum: Boolean
)(using Configuration): List[Decoder[_]] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts) =>
      summonDecoder[t](autoRecurse, derivingForSum) :: summonDecoders[ts](autoRecurse, derivingForSum)

@deprecated("Use summonDecoders(autoRecurse: Boolean) instead", "0.14.6")
private[circe] inline final def summonDecoders[T <: Tuple](using Configuration): List[Decoder[_]] =
  summonDecoders(true, false)

private[circe] inline final def summonDecoderAutoRecurse[A](using Configuration): Decoder[A] =
  summonFrom {
    case decodeA: Decoder[A] => decodeA
    case _: Mirror.Of[A]     => ConfiguredDecoder.derived[A]
  }

private[circe] inline final def summonDecoderNoAutoRecurse[A](
  inline derivingForSum: Boolean
)(using conf: Configuration): Decoder[A] =
  summonFrom {
    case decodeA: Decoder[A] => decodeA
    case m: Mirror.Of[A] =>
      inline if (derivingForSum) ConfiguredDecoder.derivedNoAutoRecursion[A]
      else error("Failed to find an instance of Decoder[" + constValue[m.MirroredLabel] + "]")
  }

private[circe] inline final def summonDecoder[A](inline autoRecurse: Boolean, inline derivingForSum: Boolean)(using
  Configuration
): Decoder[A] =
  inline if (autoRecurse) summonDecoderAutoRecurse[A] else summonDecoderNoAutoRecurse[A](derivingForSum)

@deprecated("Use summonDecoder(autoRecurse: Boolean) instead", "0.14.6")
private[circe] inline final def summonDecoder[A](using Configuration): Decoder[A] = summonDecoder(true, false)

private[circe] inline def summonSingletonCases[T <: Tuple, A](inline typeName: Any): List[A] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (h *: t) =>
      inline summonInline[Mirror.Of[h]] match
        case m: Mirror.Singleton => m.fromProduct(EmptyTuple).asInstanceOf[A] :: summonSingletonCases[t, A](typeName)
        case m: Mirror =>
          error("Enum " + codeOf(typeName) + " contains non singleton case " + codeOf(constValue[m.MirroredLabel]))
