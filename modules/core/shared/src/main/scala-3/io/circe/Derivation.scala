package io.circe

import scala.compiletime.constValue
import scala.deriving.Mirror
import Predef.genericArrayOps
import cats.data.{NonEmptyList, Validated}
import io.circe.derivation._

private[circe] trait DerivedInstance[A](
  final val name: String,
  protected[this] final val elemLabels: Array[String]
)

private[circe] trait DerivedEncoder[A](using conf: Configuration) extends DerivedInstance[A] with Encoder.AsObject[A] {
  protected[this] def elemEncoders: Array[Encoder[_]]

  final def encodeWith(index: Int)(value: Any): (String, Json) =
    (elemLabels(index), elemEncoders(index).asInstanceOf[Encoder[Any]].apply(value))
  
  final def encodedIterable(value: Product): Iterable[(String, Json)] =
    new Iterable[(String, Json)]:
      def iterator: Iterator[(String, Json)] =
        value.productIterator.zipWithIndex.map((value, index) => encodeWith(index)(value))
  
  final def encodeProduct(a: A): JsonObject =
    JsonObject.fromIterable(encodedIterable(a.asInstanceOf[Product]))
  
  final def encodeSum(index: Int, a: A): JsonObject = encodeWith(index)(a) match {
    case (k, v) => conf.discriminator match {
      case None => JsonObject.singleton(k, v)
      case Some(discriminator) => v.asObject.getOrElse(JsonObject.empty).add(discriminator, Json.fromString(k))
    }
  }
}

private[circe] trait DerivedDecoder[A](using conf: Configuration) extends DerivedInstance[A] with Decoder[A] {
  protected[this] def elemDecoders: Array[Decoder[_]]
  protected[this] def elemDefaults: Default[A]

  private def decodeGeneric[R](c: HCursor, index: Int, decode: Decoder[AnyRef] => ACursor => R)
    (withDefault: (R, AnyRef) => R): R =
    val decoder = elemDecoders(index).asInstanceOf[Decoder[AnyRef]]
    val field = c.downField(elemLabels(index))
    val baseDecodeResult = decode(decoder)(field)
    
    if conf.useDefaults then
      elemDefaults.defaults match {
        case _: EmptyTuple => baseDecodeResult
        case defaults: NonEmptyTuple =>
          defaults(index).asInstanceOf[Option[Any]] match {
            case None => baseDecodeResult
      baseDecodeResult
  
  final def decodeWith(index: Int)(c: HCursor): Decoder.Result[AnyRef] =
    decodeGeneric(c, index, _.tryDecode)(_ orElse Right(_))
  
  final def decodeAccumulatingWith(index: Int)(c: HCursor): Decoder.AccumulatingResult[AnyRef] =
    decodeGeneric(c, index, _.tryDecodeAccumulating)(_ orElse Validated.Valid(_))
  
  final def resultIterator(c: HCursor): Iterator[Decoder.Result[AnyRef]] =
    elemLabels.iterator.zipWithIndex.map((_, index) => decodeWith(index)(c))
  
  final def resultAccumulatingIterator(c: HCursor): Iterator[Decoder.AccumulatingResult[AnyRef]] =
    elemLabels.iterator.zipWithIndex.map((_, index) => decodeAccumulatingWith(index)(c))
  
  private def decodeSumGeneric[R](c: HCursor)(fail: DecodingFailure => R, decode: Decoder[A] => ACursor => R): R =
    def fromName(sumTypeName: String): R =
      elemLabels.indexOf(sumTypeName) match {
        case -1 => fail(DecodingFailure(s"type $name hasn't a class/object/case named '$sumTypeName'.", c.history))
        case index => decode(elemDecoders(index).asInstanceOf[Decoder[A]])(c)
      }
    
    conf.discriminator match {
      case Some(discriminator) =>
        val cursor = c.downField(discriminator)
        cursor.as[Option[String]] match {
          case Left(failure) => fail(failure)
          case Right(None) => fail(DecodingFailure(s"$name: could not find discriminator field '$discriminator' or its null.", cursor.history))
          case Right(Some(sumTypeName)) => fromName(sumTypeName)
        }
      case _ =>
        // Should we fail if cursor.keys contains more than one key?
        c.keys.flatMap(_.headOption) match {
          case None => fail(DecodingFailure(name, c.history))
          case Some(sumTypeName) => fromName(sumTypeName)
        }
    }
  final def decodeSum(c: HCursor): Decoder.Result[A] =
    decodeSumGeneric(c)(Left.apply, _.tryDecode)
  final def decodeSumAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
    decodeSumGeneric(c)(Validated.invalidNel, _.tryDecodeAccumulating)
  
  final def decodeProduct(c: HCursor, fromProduct: Product => A): Decoder.Result[A] =
    if (c.value.isObject) {
      val iter = resultIterator(c)
      val res = new Array[AnyRef](elemLabels.length)
      var failed: Left[DecodingFailure, _] = null
      
      var i: Int = 0
      while (iter.hasNext && (failed eq null)) {
        iter.next match
          case Right(value) => res(i) = value
          case l @ Left(_) => failed = l
        i += 1
      }
      
      if (failed eq null) {
        Right(fromProduct(Tuple.fromArray(res)))
      } else {
        failed.asInstanceOf[Decoder.Result[A]]
      }
    } else {
      Left(DecodingFailure(name, c.history))
    }
  final def decodeProductAccumulating(c: HCursor, fromProduct: Product => A): Decoder.AccumulatingResult[A] =
    if (c.value.isObject) {
      val iter = resultAccumulatingIterator(c)
      val res = new Array[AnyRef](elemLabels.length)
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
        Validated.valid(fromProduct(Tuple.fromArray(res)))
      } else {
        Validated.invalid(NonEmptyList.fromListUnsafe(failures))
      }
    } else {
      Validated.invalidNel(DecodingFailure(name, c.history))
    }
}

private[circe] trait EncoderDerivation {
  inline final def derived[A](using inline A: Mirror.Of[A], conf: Configuration = Configuration()): Encoder.AsObject[A] =
    new DerivedEncoder[A] with DerivedInstance[A](
      constValue[A.MirroredLabel],
      summonLabels[A.MirroredElemLabels].map(conf.transformNames).toArray
    ) {
      protected[this] lazy val elemEncoders: Array[Encoder[_]] = summonEncoders[A.MirroredElemTypes].toArray
      
      final def encodeObject(a: A): JsonObject = inline A match {
        case m: Mirror.ProductOf[A] => encodeProduct(a)
        case m: Mirror.SumOf[A] => encodeSum(m.ordinal(a), a)
      }
    }
}

private[circe] trait DecoderDerivation {
  inline final def derived[A](using inline A: Mirror.Of[A], conf: Configuration = Configuration()): Decoder[A] =
    new DerivedDecoder[A] with DerivedInstance[A](
      constValue[A.MirroredLabel],
      summonLabels[A.MirroredElemLabels].map(conf.transformNames).toArray,
    ):
      protected[this] lazy val elemDecoders: Array[Decoder[_]] = summonDecoders[A.MirroredElemTypes].toArray
      protected[this] lazy val elemDefaults: Default[A] = Predef.summon[Default[A]]
      
      final def apply(c: HCursor): Decoder.Result[A] =
        inline A match {
          case m: Mirror.ProductOf[A] => decodeProduct(c, m.fromProduct)
          case m: Mirror.SumOf[A] => decodeSum(c)
        }
      final override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
        inline A match {
          case m: Mirror.ProductOf[A] => decodeProductAccumulating(c, m.fromProduct)
          case m: Mirror.SumOf[A] => decodeSumAccumulating(c)
        }
}

private[circe] trait CodecDerivation {
  inline final def derived[A](using inline A: Mirror.Of[A], conf: Configuration = Configuration()): Codec.AsObject[A] =
    new Codec.AsObject[A]
        with DerivedDecoder[A]
        with DerivedEncoder[A]
        with DerivedInstance[A](
          constValue[A.MirroredLabel],
          summonLabels[A.MirroredElemLabels].map(conf.transformNames).toArray,
        ) {
      protected[this] lazy val elemEncoders: Array[Encoder[_]] = summonEncoders[A.MirroredElemTypes].toArray
      protected[this] lazy val elemDecoders: Array[Decoder[_]] = summonDecoders[A.MirroredElemTypes].toArray
      protected[this] lazy val elemDefaults: Default[A] = Predef.summon[Default[A]]

      final def encodeObject(a: A): JsonObject = inline A match {
        case m: Mirror.ProductOf[A] => encodeProduct(a)
        case m: Mirror.SumOf[A] => encodeSum(m.ordinal(a), a)
      }
      
      final def apply(c: HCursor): Decoder.Result[A] =
        inline A match {
          case m: Mirror.ProductOf[A] => decodeProduct(c, m.fromProduct)
          case m: Mirror.SumOf[A] => decodeSum(c)
        }
      
      final override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
        inline A match {
          case m: Mirror.ProductOf[A] => decodeProductAccumulating(c, m.fromProduct)
          case m: Mirror.SumOf[A] => decodeSumAccumulating(c)
        }
    }
}
