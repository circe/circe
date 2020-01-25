package io.circe

import cats.data.{NonEmptyList, Validated}
import scala.deriving.{ArrayProduct, Mirror}
import scala.collection.mutable.WrappedArray
import scala.compiletime.{constValue, erasedValue, error, summonFrom}

object Derivation {

  inline final def summonLabels[T <: Tuple]: Array[String] = summonLabelsRec[T].toArray
  inline final def summonDecoders[T <: Tuple]: Array[Decoder[_]] = summonDecodersRec[T].toArray
  inline final def summonEncoders[T <: Tuple]: Array[Encoder[_]] = summonEncodersRec[T].toArray

  inline final def summon[A]: A = summonFrom {
    case a: A => a
  }

  inline final def summonLabelsRec[T <: Tuple]: List[String] = inline erasedValue[T] match {
    case _: Unit => Nil
    case _: (t *: ts) => constValue[t].asInstanceOf[String] :: summonLabelsRec[ts]
  }

  inline final def summonDecodersRec[T <: Tuple]: List[Decoder[_]] =
    inline erasedValue[T] match {
      case _: Unit => Nil
      case _: (t *: ts) => summon[Decoder[t]] :: summonDecodersRec[ts]
    }

  inline final def summonEncodersRec[T <: Tuple]: List[Encoder[_]] =
    inline erasedValue[T] match {
      case _: Unit => Nil
      case _: (t *: ts) => summon[Encoder[t]] :: summonEncodersRec[ts]
    }
}

private[circe] trait EncoderDerivation {
  inline final def derived[A](given inline A: Mirror.Of[A]): Encoder.AsObject[A] =
    new DerivedEncoder[A]
        with DerivedInstance[A](
          constValue[A.MirroredLabel],
          Derivation.summonLabels[A.MirroredElemLabels]
        ) {
      protected[this] lazy val elemEncoders: Array[Encoder[_]] =
        Derivation.summonEncoders[A.MirroredElemTypes]

      final def encodeObject(a: A): JsonObject = inline A match {
        case m: Mirror.ProductOf[A] =>
          JsonObject.fromIterable(encodedIterable(a.asInstanceOf[Product]))
        case m: Mirror.SumOf[A] => encodeWith(m.ordinal(a))(a) match {
          case (k, v) => JsonObject.singleton(k, v)
        }
      }
    }
}

private[circe] trait DecoderDerivation {
  inline final def derived[A](given inline A: Mirror.Of[A]): Decoder[A] =
    new DerivedDecoder[A]
        with DerivedInstance[A](
          constValue[A.MirroredLabel],
          Derivation.summonLabels[A.MirroredElemLabels]
        ) {
      protected[this] lazy val elemDecoders: Array[Decoder[_]] =
        Derivation.summonDecoders[A.MirroredElemTypes]

      final def apply(c: HCursor): Decoder.Result[A] = inline A match {
        case m: Mirror.ProductOf[A] =>
          val iter = resultIterator(c)
          val res = new Array[AnyRef](elemCount)
          var failed: Left[DecodingFailure, _] = null
          var i: Int = 0

          while (iter.hasNext && (failed eq null)) {
            iter.next match {
              case Right(value) => res(i) = value
              case l @ Left(_) => failed = l
            }
            i += 1
          }

          if (failed eq null) {
            Right(m.fromProduct(new ArrayProduct(res)))
          } else {
            failed.asInstanceOf[Decoder.Result[A]]
          }
        case m: Mirror.SumOf[A] =>
          extractIndexFromWrapper(c) match {
            case -1 => Left(DecodingFailure(name, c.history))
            case index => decodeWith(index)(c).asInstanceOf[Decoder.Result[A]]
          }
      }
      final override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
        inline A match {
          case m: Mirror.ProductOf[A] =>
            val iter = resultAccumulatingIterator(c)
            val res = new Array[AnyRef](elemCount)
            val failed = List.newBuilder[DecodingFailure]
            var i: Int = 0

            while (iter.hasNext) {
              iter.next match {
                case Validated.Valid(value) => res(i) = value
                case Validated.Invalid(failures) => failed ++= failures.toList
              }
              i += 1
            }

            val failures = failed.result()
            if (failures.isEmpty) {
              Validated.valid(m.fromProduct(new ArrayProduct(res)))
            } else {
              Validated.invalid(NonEmptyList.fromListUnsafe(failures))
            }
          case m: Mirror.SumOf[A] =>
            extractIndexFromWrapper(c) match {
              case -1 => Validated.invalidNel(DecodingFailure(name, c.history))
              case index => decodeAccumulatingWith(index)(c).asInstanceOf[Decoder.AccumulatingResult[A]]
            }
        }
  }
}

private[circe] trait CodecDerivation {
  inline final def derived[A](given inline A: Mirror.Of[A]): Codec.AsObject[A] =
    new Codec.AsObject[A]
        with DerivedDecoder[A]
        with DerivedEncoder[A]
        with DerivedInstance[A](
          constValue[A.MirroredLabel],
          Derivation.summonLabels[A.MirroredElemLabels]
        ) {
      protected[this] lazy val elemDecoders: Array[Decoder[_]] =
        Derivation.summonDecoders[A.MirroredElemTypes]
      protected[this] lazy val elemEncoders: Array[Encoder[_]] =
        Derivation.summonEncoders[A.MirroredElemTypes]

      final def encodeObject(a: A): JsonObject = inline A match {
        case m: Mirror.ProductOf[A] =>
          JsonObject.fromIterable(encodedIterable(a.asInstanceOf[Product]))
        case m: Mirror.SumOf[A] => encodeWith(m.ordinal(a))(a) match {
          case (k, v) => JsonObject.singleton(k, v)
        }
      }

      final def apply(c: HCursor): Decoder.Result[A] = inline A match {
        case m: Mirror.ProductOf[A] =>
          val iter = resultIterator(c)
          val res = new Array[AnyRef](elemCount)
          var failed: Left[DecodingFailure, _] = null
          var i: Int = 0

          while (iter.hasNext && (failed eq null)) {
            iter.next match {
              case Right(value) => res(i) = value
              case l @ Left(_) => failed = l
            }
            i += 1
          }

          if (failed eq null) {
            Right(m.fromProduct(new ArrayProduct(res)))
          } else {
            failed.asInstanceOf[Decoder.Result[A]]
          }
        case m: Mirror.SumOf[A] =>
          extractIndexFromWrapper(c) match {
            case -1 => Left(DecodingFailure(name, c.history))
            case index => decodeWith(index)(c).asInstanceOf[Decoder.Result[A]]
          }
      }
      final override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
        inline A match {
          case m: Mirror.ProductOf[A] =>
            val iter = resultAccumulatingIterator(c)
            val res = new Array[AnyRef](elemCount)
            val failed = List.newBuilder[DecodingFailure]
            var i: Int = 0

            while (iter.hasNext) {
              iter.next match {
                case Validated.Valid(value) => res(i) = value
                case Validated.Invalid(failures) => failed ++= failures.toList
              }
              i += 1
            }

            val failures = failed.result()
            if (failures.isEmpty) {
              Validated.valid(m.fromProduct(new ArrayProduct(res)))
            } else {
              Validated.invalid(NonEmptyList.fromListUnsafe(failures))
            }
          case m: Mirror.SumOf[A] =>
            extractIndexFromWrapper(c) match {
              case -1 => Validated.invalidNel(DecodingFailure(name, c.history))
              case index => decodeAccumulatingWith(index)(c).asInstanceOf[Decoder.AccumulatingResult[A]]
            }
        }
  }
}

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
