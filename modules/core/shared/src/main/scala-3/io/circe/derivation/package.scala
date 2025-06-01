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
import io.circe.{ Decoder, Encoder }

private[circe] inline final def summonLabels[T <: Tuple]: List[String] =
  loopUnrolledNoArg[String, T](
    constString
  )

@deprecated("Use summonEncoders(derivingForSum: Boolean) instead", "0.14.7")
private[circe] inline final def summonEncoders[T <: Tuple](using Configuration): List[Encoder[_]] =
  summonEncoders(false)

private[circe] inline final def summonEncoders[T <: Tuple](inline derivingForSum: Boolean)(using
  Configuration
): List[Encoder[_]] =
  loopUnrolledNoArg[Encoder[_], T](
    inline if (derivingForSum) new EncoderDeriveSum else new EncoderNotDeriveSum
  )

@deprecated("Use summonEncoder(derivingForSum: Boolean) instead", "0.14.7")
private[circe] inline final def summonEncoder[A](using Configuration): Encoder[A] =
  summonEncoder(false)

private[circe] inline final def summonEncoder[A](inline derivingForSum: Boolean)(using Configuration): Encoder[A] =
  summonFrom {
    case encodeA: Encoder[A] => encodeA
    case m: Mirror.Of[A]     =>
      inline if (derivingForSum) ConfiguredEncoder.derived[A]
      else error("Failed to find an instance of Encoder[" + typeName[A] + "]")
  }

@deprecated("Use summonDecoders(derivingForSum: Boolean) instead", "0.14.7")
private[circe] inline final def summonDecoders[T <: Tuple](using Configuration): List[Decoder[_]] =
  summonDecoders(false)

private[circe] inline final def summonDecoders[T <: Tuple](inline derivingForSum: Boolean)(using
  Configuration
): List[Decoder[_]] =
  loopUnrolledNoArg[Decoder[_], T](
    inline if (derivingForSum) new DecoderDeriveSum else new DecoderNotDeriveSum
  )

@deprecated("Use summonDecoder(derivingForSum: Boolean) instead", "0.14.7")
private[circe] inline final def summonDecoder[A](using Configuration): Decoder[A] =
  summonDecoder(false)

private[circe] inline final def summonDecoder[A](inline derivingForSum: Boolean)(using Configuration): Decoder[A] =
  summonFrom {
    case decodeA: Decoder[A] => decodeA
    case m: Mirror.Of[A]     =>
      inline if (derivingForSum) ConfiguredDecoder.derived[A]
      else error("Failed to find an instance of Decoder[" + typeName[A] + "]")
  }

private[circe] final case class SingletonCase[A](label: String, value: A)

private[circe] inline final def summonSingletonCase[T, A](inline typeName: Any): List[SingletonCase[A]] =
  inline summonInline[Mirror.Of[T]] match
    case m: Mirror.Singleton =>
      List(
        SingletonCase(
          value = m.fromProduct(EmptyTuple).asInstanceOf[A],
          label = constValue[m.MirroredLabel]
        )
      )
    case m: Mirror.SumOf[T] => summonSingletonCases[m.MirroredElemTypes, A](typeName)
    case m: Mirror          =>
      error("Enum " + codeOf(typeName) + " contains non singleton case " + codeOf(constValue[m.MirroredLabel]))

private[circe] inline def summonSingletonCases[T <: Tuple, A](inline typeName: Any): List[SingletonCase[A]] =
  loopUnrolled[List[SingletonCase[A]], Any, T](new SummonSingleton[A], typeName).flatten

private[circe] inline final def loopUnrolled[A, Arg, T <: Tuple](f: Inliner[A, Arg], inline arg: Arg): List[A] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (h1 *: h2 *: h3 *: h4 *: h5 *: h6 *: h7 *: h8 *: h9 *: h10 *: h11 *: h12 *: h13 *: h14 *: h15 *: h16 *:
          ts) =>
      f[h1](arg) :: f[h2](arg) :: f[h3](arg) :: f[h4](arg) :: f[h5](arg) :: f[h6](arg) :: f[h7](arg) :: f[h8](arg)
        :: f[h9](arg) :: f[h10](arg) :: f[h11](arg) :: f[h12](arg) :: f[h13](arg) :: f[h14](arg) :: f[h15](arg) :: f[
          h16
        ](arg)
        :: loopUnrolled[A, Arg, ts](f, arg)
    case _: (h1 *: h2 *: h3 *: h4 *: ts) =>
      f[h1](arg) :: f[h2](arg) :: f[h3](arg) :: f[h4](arg) :: loopUnrolled[A, Arg, ts](f, arg)
    case _: (h *: ts) => f[h](arg) :: loopUnrolled[A, Arg, ts](f, arg)

private[circe] inline def loopUnrolledNoArg[A, T <: Tuple](f: Inliner[A, Unit]): List[A] =
  loopUnrolled[A, Unit, T](f, ())

sealed trait Inliner[A, Arg]:
  inline def apply[T](inline arg: Arg): A

object constString extends Inliner[String, Unit]:
  inline def apply[T](inline arg: Unit): String = constValue[T].asInstanceOf[String]

class EncoderDeriveSum(using config: Configuration) extends Inliner[Encoder[_], Unit]:
  inline def apply[T](inline arg: Unit): Encoder[?] = summonEncoder[T](true)

class EncoderNotDeriveSum(using config: Configuration) extends Inliner[Encoder[_], Unit]:
  inline def apply[T](inline arg: Unit): Encoder[?] = summonEncoder[T](false)

class DecoderDeriveSum(using Configuration) extends Inliner[Decoder[_], Unit]:
  inline def apply[T](inline arg: Unit): Decoder[?] = summonDecoder[T](true)

class DecoderNotDeriveSum(using Configuration) extends Inliner[Decoder[_], Unit]:
  inline def apply[T](inline arg: Unit): Decoder[?] = summonDecoder[T](false)

class SummonSingleton[A] extends Inliner[List[SingletonCase[A]], Any]:
  inline def apply[T](inline typeName: Any): List[SingletonCase[A]] = summonSingletonCase[T, A](typeName)
