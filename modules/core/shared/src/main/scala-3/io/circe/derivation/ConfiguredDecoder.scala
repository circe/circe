package io.circe.derivation

import scala.deriving.Mirror
import scala.compiletime.constValue
import Predef.genericArrayOps
import cats.data.{NonEmptyList, Validated}
import io.circe.{Decoder, DecodingFailure, ACursor, HCursor}

trait ConfiguredDecoder[A](using conf: Configuration) extends Decoder[A], DerivedInstance[A] {
  def elemDecoders: Array[Decoder[_]]
  def elemDefaults: Default[A]

  private def decodeSumElement[R](c: HCursor)(fail: DecodingFailure => R, decode: Decoder[A] => ACursor => R): R = {
    def fromName(sumTypeName: String, cursor: ACursor): R =
      elemLabels.indexOf(sumTypeName) match {
        case -1 => fail(DecodingFailure(s"type $name hasn't a class/object/case named '$sumTypeName'.", cursor.history))
        case index => decode(elemDecoders(index).asInstanceOf[Decoder[A]])(cursor)
      }
    
    conf.discriminator match {
      case Some(discriminator) =>
        val cursor = c.downField(discriminator)
        cursor.as[Option[String]] match {
          case Left(failure) => fail(failure)
          case Right(None) => fail(DecodingFailure(s"$name: could not find discriminator field '$discriminator' or its null.", cursor.history))
          case Right(Some(sumTypeName)) => fromName(sumTypeName, c)
        }
      case _ =>
        // Should we fail if cursor.keys contains more than one key?
        c.keys.flatMap(_.headOption) match {
          case None => fail(DecodingFailure(name, c.history))
          case Some(sumTypeName) => fromName(sumTypeName, c.downField(sumTypeName))
        }
    }
  }
  final def decodeSum(c: HCursor): Decoder.Result[A] =
    decodeSumElement(c)(Left.apply, _.tryDecode)
  final def decodeSumAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
    decodeSumElement(c)(Validated.invalidNel, _.tryDecodeAccumulating)
  
  private def decodeProductElement[R](c: HCursor, index: Int, decode: Decoder[Any] => ACursor => R)
    (withDefault: (R, Any) => R): R = {
    val decoder = elemDecoders(index).asInstanceOf[Decoder[Any]]
    val field = c.downField(elemLabels(index))
    val result = decode(decoder)(field)
    
    if (conf.useDefaults) {
      elemDefaults.defaultAt(index) match {
        case None => result
        case Some(default) => withDefault(result, default)
      }
    } else {
      result
    }
  }

  final def decodeProduct(c: HCursor, fromProduct: Product => A): Decoder.Result[A] =
    if (c.value.isObject) {
      val res = new Array[Any](elemLabels.length)
      var failed: Left[DecodingFailure, _] = null
      
      var index: Int = 0
      while (index < elemLabels.length && (failed eq null)) {
        decodeProductElement(c, index, _.tryDecode)(withDefault = _ orElse Right(_)) match
          case Right(value) => res(index) = value
          case l @ Left(_) => failed = l
        index += 1
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
      val res = new Array[Any](elemLabels.length)
      val failed = List.newBuilder[DecodingFailure]
      
      var index: Int = 0
      while (index < elemLabels.length) {
        decodeProductElement(c, index, _.tryDecodeAccumulating)(withDefault = _ orElse Validated.Valid(_)) match {
          case Validated.Valid(value) => res(index) = value
          case Validated.Invalid(failures) => failed ++= failures.toList
        }
        index += 1
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
object ConfiguredDecoder {
  inline final def derived[A](using conf: Configuration = Configuration.default)(using inline A: Mirror.Of[A]): ConfiguredDecoder[A] =
    new ConfiguredDecoder[A] with DerivedInstance[A](
      constValue[A.MirroredLabel],
      summonLabels[A.MirroredElemLabels].map(conf.transformNames).toArray,
    ) {
      lazy val elemDecoders: Array[Decoder[_]] = summonDecoders[A.MirroredElemTypes].toArray
      lazy val elemDefaults: Default[A] = Predef.summon[Default[A]]
      
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
  
  inline final def derive[A: Mirror.Of](
    transformNames: String => String = Configuration.default.transformNames,
    useDefaults: Boolean = Configuration.default.useDefaults,
    discriminator: Option[String] = Configuration.default.discriminator,
  ): ConfiguredDecoder[A] = {
    given Configuration = Configuration(transformNames, useDefaults, discriminator)
    derived[A]
  }
}
