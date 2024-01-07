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
import scala.collection.immutable.SortedSet
import io.circe.{ Codec, Decoder, Encoder, KeyDecoder, KeyEncoder, Nullable }
import cats.data.{ Chain, NonEmptyChain, NonEmptyList, NonEmptyMap, NonEmptySet, NonEmptyVector }
import cats.kernel.Order

private[circe] inline def summonLabels[T <: Tuple]: List[String] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts)  => constValue[t].asInstanceOf[String] :: summonLabels[ts]

private[circe] inline def summonEncoders[T <: Tuple](using Configuration): List[Encoder[_]] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts)  => summonEncoder[t] :: summonEncoders[ts]

private[circe] inline def summonEncoder[A](using Configuration): Encoder[A] =
  inline erasedValue[A] match
//    case _: String      => Encoder.encodeString.asInstanceOf[Encoder[A]]
//    case _: Option[t]   => Encoder.encodeOption(summonEncoder[t]).asInstanceOf[Encoder[A]]
//    case _: Nullable[t] => Encoder.encodeNullable(summonEncoder[t]).asInstanceOf[Encoder[A]]
//    case _: List[t]     => Encoder.encodeList(summonEncoder[t]).asInstanceOf[Encoder[A]]
//    case _: Vector[t]   => Encoder.encodeVector(summonEncoder[t]).asInstanceOf[Encoder[A]]
//    case _: Seq[t]      => Encoder.encodeSeq(summonEncoder[t]).asInstanceOf[Encoder[A]]
//    case _: SortedSet[t] =>
//      Encoder.encodeSet(summonEncoder[t]).contramap[SortedSet[t]](_.unsorted).asInstanceOf[Encoder[A]]
//    case _: Set[t]          => Encoder.encodeSet(summonEncoder[t]).asInstanceOf[Encoder[A]]
//    case _: Chain[t]        => Encoder.encodeChain(summonEncoder[t]).asInstanceOf[Encoder[A]]
//    case _: NonEmptyList[t] => Encoder.encodeNonEmptyList(summonEncoder[t]).asInstanceOf[Encoder[A]]
//    case _: NonEmptySet[t] => // NonEmptySet is a NewType, matching does not seem to work
//      Encoder.encodeNonEmptySet(summonEncoder[t]).asInstanceOf[Encoder[A]]
//    case _: NonEmptyVector[t] => Encoder.encodeNonEmptyVector(summonEncoder[t]).asInstanceOf[Encoder[A]]
//    case _: Map[k, t]         => Encoder.encodeMap(summonKeyEncoder[k], summonEncoder[t]).asInstanceOf[Encoder[A]]
//    case _: NonEmptyMap[k, t] => // NonEmptyMap is a NewType, matching does not seem to work
//      Encoder.encodeNonEmptyMap(summonKeyEncoder[k], summonEncoder[t]).asInstanceOf[Encoder[A]]
//    case _: NonEmptyChain[t] => // NonEmptyChain is a NewType, matching does not seem to work
//      Encoder.encodeNonEmptyChain(summonEncoder[t]).asInstanceOf[Encoder[A]]
    case _ =>
      summonFrom {
        case encodeA: Encoder[A] => encodeA
        case _: Mirror.Of[A]     => ConfiguredEncoder.derived[A]
      }

private[circe] inline def summonDecoders[T <: Tuple](using Configuration): List[Decoder[_]] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (t *: ts)  => summonDecoder[t] :: summonDecoders[ts]

private[circe] inline def summonDecoder[A](using Configuration): Decoder[A] =
  inline erasedValue[A] match
//    case _: String      => Decoder.decodeString.asInstanceOf[Decoder[A]]
//    case _: Option[t]   => Decoder.decodeOption(summonDecoder[t]).asInstanceOf[Decoder[A]]
//    case _: Nullable[t] => Decoder.decodeNullable(summonDecoder[t]).asInstanceOf[Decoder[A]]
//    case _: List[t]     => Decoder.decodeList(summonDecoder[t]).asInstanceOf[Decoder[A]]
//    case _: Vector[t]   => Decoder.decodeVector(summonDecoder[t]).asInstanceOf[Decoder[A]]
//    case _: Seq[t]      => Decoder.decodeSeq(summonDecoder[t]).asInstanceOf[Decoder[A]]
//    case _: SortedSet[t] =>
//      Decoder.decodeSet(summonDecoder[t]).map(SortedSet.from(_)(summonOrdering[t])).asInstanceOf[Decoder[A]]
//    case _: Set[t]          => Decoder.decodeSet(summonDecoder[t]).asInstanceOf[Decoder[A]]
//    case _: Chain[t]        => Decoder.decodeChain(summonDecoder[t]).asInstanceOf[Decoder[A]]
//    case _: NonEmptyList[t] => Decoder.decodeNonEmptyList(summonDecoder[t]).asInstanceOf[Decoder[A]]
//    case _: NonEmptySet[t] => // NonEmptySet is a NewType, matching does not seem to work
//      Decoder.decodeNonEmptySet(summonDecoder[t], summonOrder[t]).asInstanceOf[Decoder[A]]
//    case _: NonEmptyVector[t] => Decoder.decodeNonEmptyVector(summonDecoder[t]).asInstanceOf[Decoder[A]]
//    case _: Map[k, t]         => Decoder.decodeMap(summonKeyDecoder[k], summonDecoder[t]).asInstanceOf[Decoder[A]]
//    case _: NonEmptyMap[k, t] => // NonEmptyMap is a NewType, matching does not seem to work
//      Decoder.decodeNonEmptyMap(summonKeyDecoder[k], summonOrder[k], summonDecoder[t]).asInstanceOf[Decoder[A]]
//    case _: NonEmptyChain[t] => // NonEmptyChain is a NewType, matching does not seem to work
//      Decoder.decodeNonEmptyChain(summonDecoder[t]).asInstanceOf[Decoder[A]]
    case _ =>
      summonFrom {
        case decodeA: Decoder[A] => decodeA
        case _: Mirror.Of[A]     => ConfiguredDecoder.derived[A]
      }

private[circe] inline def summonOrder[A]: Order[A] = summonFrom { case orderA: Order[A] => orderA }

private[circe] inline def summonOrdering[A]: Ordering[A] = summonFrom { case orderA: Ordering[A] => orderA }

private[circe] inline def summonKeyDecoder[A]: KeyDecoder[A] = summonFrom {
  case decodeK: KeyDecoder[A] => decodeK
}
private[circe] inline def summonKeyEncoder[A]: KeyEncoder[A] = summonFrom {
  case encodeK: KeyEncoder[A] => encodeK
}

private[circe] inline def summonSingletonCases[T <: Tuple, A](inline typeName: Any): List[A] =
  inline erasedValue[T] match
    case _: EmptyTuple => Nil
    case _: (h *: t) =>
      inline summonInline[Mirror.Of[h]] match
        case m: Mirror.Singleton => m.fromProduct(EmptyTuple).asInstanceOf[A] :: summonSingletonCases[t, A](typeName)
        case m: Mirror =>
          error("Enum " + codeOf(typeName) + " contains non singleton case " + codeOf(constValue[m.MirroredLabel]))
