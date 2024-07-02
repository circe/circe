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

package io.circe

import scala.compiletime.constValue
import scala.deriving.Mirror
import Predef.genericArrayOps
import cats.data.{ NonEmptyList, Validated }
import io.circe.derivation._

@deprecated(since = "0.14.4")
object Derivation {
  inline final def summonLabels[T <: Tuple]: Array[String] = summonLabelsRec[T].toArray
  inline final def summonDecoders[T <: Tuple]: Array[Decoder[_]] =
    derivation.summonDecoders[T](derivingForSum = false)(using Configuration.default).toArray
  inline final def summonEncoders[T <: Tuple]: Array[Encoder[_]] =
    derivation.summonEncoders[T](derivingForSum = false)(using Configuration.default).toArray

  inline final def summonEncoder[A]: Encoder[A] = derivation.summonEncoder[A](false)(using Configuration.default)
  inline final def summonDecoder[A]: Decoder[A] = derivation.summonDecoder[A](false)(using Configuration.default)

  inline final def summonLabelsRec[T <: Tuple]: List[String] = derivation.summonLabels[T]
  inline final def summonDecodersRec[T <: Tuple]: List[Decoder[_]] =
    derivation.summonDecoders[T](derivingForSum = false)(using Configuration.default)
  inline final def summonEncodersRec[T <: Tuple]: List[Encoder[_]] =
    derivation.summonEncoders[T](derivingForSum = false)(using Configuration.default)
}

@deprecated(since = "0.14.4")
private[circe] trait DerivedInstance[A](
  final val name: String,
  protected[this] final val elemLabels: Array[String]
) {
  final def elemCount: Int = elemLabels.length

  protected[this] final def findLabel(name: String): Int = {
    var i = 0
    while (i < elemCount) {
      if (elemLabels(i) == name) return i
      i += 1
    }
    return -1
  }
}
@deprecated(since = "0.14.4")
private[circe] trait DerivedEncoder[A] extends DerivedInstance[A] with Encoder.AsObject[A] {
  protected[this] def elemEncoders: Array[Encoder[_]]

  final def encodeWith(index: Int)(value: Any): (String, Json) =
    (elemLabels(index), elemEncoders(index).asInstanceOf[Encoder[Any]].apply(value))

  final def encodedIterable(value: Product): Iterable[(String, Json)] =
    new Iterable[(String, Json)] {
      def iterator: Iterator[(String, Json)] = new Iterator[(String, Json)] {
        private[this] val elems: Iterator[Any] = value.productIterator
        private[this] var index: Int = 0

        def hasNext: Boolean = elems.hasNext

        def next(): (String, Json) = {
          val field = encodeWith(index)(elems.next())
          index += 1
          field
        }
      }
    }
}
@deprecated(since = "0.14.4")
private[circe] trait DerivedDecoder[A] extends DerivedInstance[A] with Decoder[A] {
  protected[this] def elemDecoders: Array[Decoder[_]]

  final def decodeWith(index: Int)(c: HCursor): Decoder.Result[AnyRef] =
    elemDecoders(index).asInstanceOf[Decoder[AnyRef]].tryDecode(c.downField(elemLabels(index)))

  final def decodeAccumulatingWith(index: Int)(c: HCursor): Decoder.AccumulatingResult[AnyRef] =
    elemDecoders(index).asInstanceOf[Decoder[AnyRef]].tryDecodeAccumulating(c.downField(elemLabels(index)))

  final def resultIterator(c: HCursor): Iterator[Decoder.Result[AnyRef]] =
    new Iterator[Decoder.Result[AnyRef]] {
      private[this] var i: Int = 0

      def hasNext: Boolean = i < elemCount

      def next: Decoder.Result[AnyRef] = {
        val result = decodeWith(i)(c)
        i += 1
        result
      }
    }

  final def resultAccumulatingIterator(c: HCursor): Iterator[Decoder.AccumulatingResult[AnyRef]] =
    new Iterator[Decoder.AccumulatingResult[AnyRef]] {
      private[this] var i: Int = 0

      def hasNext: Boolean = i < elemCount

      def next: Decoder.AccumulatingResult[AnyRef] = {
        val result = decodeAccumulatingWith(i)(c)
        i += 1
        result
      }
    }

  final def extractIndexFromWrapper(c: HCursor): Int = c.keys match {
    case Some(keys) =>
      val iter = keys.iterator
      if (iter.hasNext) {
        val key = iter.next
        if (iter.hasNext) {
          -1
        } else {
          findLabel(key)
        }
      } else {
        -1
      }
    case None => -1
  }
}

private[circe] trait EncoderDerivation:
  inline final def derived[A](using inline A: Mirror.Of[A]): Encoder.AsObject[A] =
    ConfiguredEncoder.derived[A](using Configuration.default)
  inline final def derivedConfigured[A](using
    inline A: Mirror.Of[A],
    inline configuration: Configuration
  ): Encoder.AsObject[A] =
    ConfiguredEncoder.derived[A]

private[circe] trait EncoderDerivationRelaxed:
  inline final def derived[A](using
    inline A: Mirror.Of[A],
    configuration: Configuration = Configuration.default
  ): Encoder.AsObject[A] =
    ConfiguredEncoder.derived[A]

private[circe] trait DecoderDerivation:
  inline final def derived[A](using inline A: Mirror.Of[A]): Decoder[A] =
    ConfiguredDecoder.derived[A](using Configuration.default)
  inline final def derivedConfigured[A](using inline A: Mirror.Of[A], inline configuration: Configuration): Decoder[A] =
    ConfiguredDecoder.derived[A]

private[circe] trait CodecDerivation:
  inline final def derived[A](using inline A: Mirror.Of[A]): Codec.AsObject[A] =
    ConfiguredCodec.derived[A](using Configuration.default)
  inline final def derivedConfigured[A](using
    inline A: Mirror.Of[A],
    inline configuration: Configuration
  ): Codec.AsObject[A] =
    ConfiguredCodec.derived[A]

private[circe] trait CodecDerivationRelaxed:
  inline final def derived[A](using
    inline A: Mirror.Of[A],
    configuration: Configuration = Configuration.default
  ): Codec.AsObject[A] =
    ConfiguredCodec.derived[A]
