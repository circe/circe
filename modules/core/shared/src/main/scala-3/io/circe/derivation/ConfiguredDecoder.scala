package io.circe.derivation

import scala.deriving.Mirror
import scala.compiletime.constValue
import Predef.genericArrayOps
import cats.data.{ NonEmptyList, Validated }
import io.circe.{ ACursor, Decoder, DecodingFailure, HCursor }
import io.circe.DecodingFailure.Reason.WrongTypeExpectation
import cats.implicits.*
import scala.collection.immutable.Map

trait ConfiguredDecoder[A](using conf: Configuration) extends Decoder[A]:
  val name: String
  lazy val elemLabels: List[String]
  lazy val elemDecoders: List[Decoder[?]]
  lazy val elemDefaults: Default[A]
  lazy val constructorNames: List[String] = elemLabels.map(conf.transformConstructorNames)

  private lazy val decodersDict: Map[String, Decoder[?]] = {
    def findDecoderDict(p: (String, Decoder[?])): List[(String, Decoder[?])] =
      p._2 match {
        case cd: ConfiguredDecoder[?] with SumOrProduct if cd.isSum =>
          cd.constructorNames.zip(cd.elemDecoders).flatMap(findDecoderDict)
        case _ => List(p)
      }
    constructorNames.zip(elemDecoders).flatMap(findDecoderDict).toMap
  }

  private def strictDecodingFailure(c: HCursor, message: String): DecodingFailure =
    DecodingFailure(s"Strict decoding $name - $message", c.history)

  /** Decodes a class/object/case of a Sum type handling discriminator and strict decoding. */
  private def decodeSumElement[R](c: HCursor)(fail: DecodingFailure => R, decode: Decoder[A] => ACursor => R): R =

    def fromName(sumTypeName: String, cursor: ACursor): R =
      decodersDict
        .get(sumTypeName)
        .fold(
          fail(DecodingFailure(s"type $name has no class/object/case named '$sumTypeName'.", cursor.history))
        ) { decoder =>
          decode(decoder.asInstanceOf[Decoder[A]])(cursor)
        }

    conf.discriminator match
      case Some(discriminator) =>
        val cursor = c.downField(discriminator)
        cursor.as[Option[String]] match
          case Left(failure) => fail(failure)
          case Right(None) =>
            fail(
              DecodingFailure(
                s"$name: could not find discriminator field '$discriminator' or its null.",
                cursor.history
              )
            )
          case Right(Some(sumTypeName)) => fromName(sumTypeName, c)
      case _ =>
        c.keys match
          case None => fail(DecodingFailure(WrongTypeExpectation("object", c.value), c.history))
          case Some(keys) =>
            val iter = keys.iterator
            if !iter.hasNext then
              fail(DecodingFailure(WrongTypeExpectation("non-empty json object", c.value), c.history))
            else
              val sumTypeName = iter.next
              if iter.hasNext && conf.strictDecoding then
                fail(
                  strictDecodingFailure(
                    c,
                    s"expected a single key json object with one of: ${constructorNames.iterator.mkString(", ")}."
                  )
                )
              else fromName(sumTypeName, c.downField(sumTypeName))

  final def decodeSum(c: HCursor): Decoder.Result[A] =
    decodeSumElement(c)(Left.apply, _.tryDecode)
  final def decodeSumAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
    decodeSumElement(c)(Validated.invalidNel, _.tryDecodeAccumulating)

  /** Decodes a single element of a product, handling its default value (if it exists). */
  private def decodeProductElement[R](
    c: HCursor,
    index: Int,
    decode: Decoder[Any] => ACursor => R,
    withDefault: (R, ACursor, Any) => R
  ): R =
    val decoder = elemDecoders(index).asInstanceOf[Decoder[Any]]
    val cursor = c.downField(conf.transformMemberNames(elemLabels(index)))
    val result = decode(decoder)(cursor)

    if conf.useDefaults then
      elemDefaults.defaultAt(index) match
        case None          => result
        case Some(default) => withDefault(result, cursor, default)
    else result

  /** Ensures cursor is a json object, handles strict decoding. */
  private def decodeProductBase[R](
    c: HCursor,
    fail: DecodingFailure => R,
    strictFail: (List[String], IndexedSeq[String]) => R
  )(decodeProduct: => R): R =
    c.value.isObject match
      case false                        => fail(DecodingFailure(WrongTypeExpectation("object", c.value), c.history))
      case true if !conf.strictDecoding => decodeProduct
      case true =>
        val expectedFields = elemLabels.toIndexedSeq.map(conf.transformMemberNames) ++ conf.discriminator
        val expectedFieldsSet = expectedFields.toSet
        val unexpectedFields = c.keys.map(_.toList.filterNot(expectedFieldsSet)).getOrElse(Nil)
        if unexpectedFields.nonEmpty then strictFail(unexpectedFields, expectedFields)
        else decodeProduct

  final def decodeProduct(c: HCursor, fromProduct: Product => A): Decoder.Result[A] =
    def strictFail(unexpectedFields: List[String], expectedFields: IndexedSeq[String]): Decoder.Result[A] =
      Left(
        strictDecodingFailure(
          c,
          s"unexpected fields: ${unexpectedFields.mkString(", ")}; valid fields: ${expectedFields.mkString(", ")}."
        )
      )

    decodeProductBase(c, Left.apply, strictFail) {
      val res = new Array[Any](elemLabels.length)
      var failed: Left[DecodingFailure, _] | Null = null

      def withDefault(result: Decoder.Result[Any], cursor: ACursor, default: Any): Decoder.Result[Any] = result match
        case r @ Right(_) if r.ne(Decoder.keyMissingNone)                      => r
        case l @ Left(_) if cursor.succeeded && !cursor.focus.exists(_.isNull) => l
        case _                                                                 => Right(default)

      var index = 0
      while index < elemLabels.length && (failed eq null) do
        decodeProductElement(c, index, _.tryDecode, withDefault) match
          case Right(value) => res(index) = value
          case l @ Left(_)  => failed = l
        index += 1
      end while

      if failed eq null then Right(fromProduct(Tuple.fromArray(res)))
      else failed.asInstanceOf[Decoder.Result[A]]
    }
  final def decodeProductAccumulating(c: HCursor, fromProduct: Product => A): Decoder.AccumulatingResult[A] =
    def strictFail(unexpectedFields: List[String], expectedFields: IndexedSeq[String]): Decoder.AccumulatingResult[A] =
      val failures = unexpectedFields.map { field =>
        strictDecodingFailure(c, s"unexpected field: $field; valid fields: ${expectedFields.mkString(", ")}.")
      }
      Validated.invalid(NonEmptyList.fromListUnsafe(failures))

    decodeProductBase(c, Validated.invalidNel, strictFail) {
      val res = new Array[Any](elemLabels.length)
      val failed = List.newBuilder[DecodingFailure]

      def withDefault(
        result: Decoder.AccumulatingResult[Any],
        cursor: ACursor,
        default: Any
      ): Decoder.AccumulatingResult[Any] = result match
        case v @ Validated.Valid(_) if v.ne(Decoder.keyMissingNoneAccumulating)             => v
        case i @ Validated.Invalid(_) if cursor.succeeded && !cursor.focus.exists(_.isNull) => i
        case _                                                                              => Validated.Valid(default)

      var index = 0
      while index < elemLabels.length do
        decodeProductElement(c, index, _.tryDecodeAccumulating, withDefault) match
          case Validated.Valid(value)      => res(index) = value
          case Validated.Invalid(failures) => failed ++= failures.toList
        index += 1
      end while

      val failures = failed.result()
      if failures.isEmpty then Validated.valid(fromProduct(Tuple.fromArray(res)))
      else Validated.invalid(NonEmptyList.fromListUnsafe(failures))
    }

object ConfiguredDecoder:
  inline final def derived[A](using conf: Configuration)(using
    inline mirror: Mirror.Of[A]
  ): ConfiguredDecoder[A] =
    new ConfiguredDecoder[A] with SumOrProduct:
      val name = constValue[mirror.MirroredLabel]
      lazy val elemLabels: List[String] = summonLabels[mirror.MirroredElemLabels]
      lazy val elemDecoders: List[Decoder[?]] = summonDecoders[mirror.MirroredElemTypes]
      lazy val elemDefaults: Default[A] = Predef.summon[Default[A]]

      lazy val isSum: Boolean =
        inline mirror match
          case _: Mirror.ProductOf[A] => false
          case _: Mirror.SumOf[A]     => true

      final def apply(c: HCursor): Decoder.Result[A] =
        inline mirror match
          case product: Mirror.ProductOf[A] => decodeProduct(c, product.fromProduct)
          case _: Mirror.SumOf[A]           => decodeSum(c)
      final override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
        inline mirror match
          case product: Mirror.ProductOf[A] => decodeProductAccumulating(c, product.fromProduct)
          case _: Mirror.SumOf[A]           => decodeSumAccumulating(c)

  inline final def derive[A: Mirror.Of](
    transformMemberNames: String => String = Configuration.default.transformMemberNames,
    transformConstructorNames: String => String = Configuration.default.transformConstructorNames,
    useDefaults: Boolean = Configuration.default.useDefaults,
    discriminator: Option[String] = Configuration.default.discriminator,
    strictDecoding: Boolean = Configuration.default.strictDecoding
  ): ConfiguredDecoder[A] =
    derived[A](using
      Configuration(transformMemberNames, transformConstructorNames, useDefaults, discriminator, strictDecoding)
    )
