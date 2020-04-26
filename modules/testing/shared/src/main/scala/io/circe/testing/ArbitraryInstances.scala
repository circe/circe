package io.circe.testing

import cats.instances.list._
import io.circe.{
  Decoder,
  DecodingFailure,
  Encoder,
  Json,
  JsonBiggerDecimal,
  JsonNumber,
  JsonObject,
  KeyDecoder,
  KeyEncoder
}
import io.circe.numbers.BiggerDecimal
import io.circe.numbers.testing.{ IntegralString, JsonNumberString }
import org.scalacheck.{ Arbitrary, Cogen, Gen }

trait ArbitraryInstances extends ArbitraryJsonNumberTransformer with CogenInstances with ShrinkInstances {

  /**
   * The maximum depth of a generated JSON value.
   */
  protected def maxJsonDepth: Int = 5

  /**
   * The maximum number of values in a generated JSON array.
   */
  protected def maxJsonArraySize: Int = 10

  /**
   * The maximum number of key-value pairs in a generated JSON object.
   */
  protected def maxJsonObjectSize: Int = 10

  implicit val arbitraryBiggerDecimal: Arbitrary[BiggerDecimal] = Arbitrary(
    Gen.oneOf(
      Arbitrary.arbitrary[JsonNumberString].map(s => BiggerDecimal.parseBiggerDecimalUnsafe(s.value)),
      Arbitrary.arbitrary[Long].map(BiggerDecimal.fromLong),
      Arbitrary.arbitrary[Double].map(BiggerDecimal.fromDoubleUnsafe),
      Arbitrary.arbitrary[BigInt].map(_.underlying).map(BiggerDecimal.fromBigInteger),
      Arbitrary.arbitrary[BigDecimal].map(_.underlying).map(BiggerDecimal.fromBigDecimal),
      Gen.const(BiggerDecimal.NegativeZero)
    )
  )

  implicit val arbitraryJsonNumber: Arbitrary[JsonNumber] = Arbitrary(
    Gen
      .oneOf(
        Arbitrary.arbitrary[IntegralString].map(input => JsonNumber.fromDecimalStringUnsafe(input.value)),
        Arbitrary.arbitrary[JsonNumberString].map(input => JsonNumber.fromDecimalStringUnsafe(input.value)),
        Arbitrary.arbitrary[BiggerDecimal].map(input => JsonBiggerDecimal(input, input.toString)),
        Arbitrary.arbitrary[BigDecimal].map(Json.fromBigDecimal(_).asNumber.get),
        Arbitrary.arbitrary[BigInt].map(Json.fromBigInt(_).asNumber.get),
        Arbitrary.arbitrary[Long].map(Json.fromLong(_).asNumber.get),
        Arbitrary.arbitrary[Double].map(Json.fromDoubleOrString(_).asNumber.get),
        Arbitrary.arbitrary[Float].map(Json.fromFloatOrString(_).asNumber.get)
      )
      .map(transformJsonNumber)
  )

  private[this] val genNull: Gen[Json] = Gen.const(Json.Null)
  private[this] val genBool: Gen[Json] = Arbitrary.arbitrary[Boolean].map(Json.fromBoolean)
  private[this] val genString: Gen[Json] = Arbitrary.arbitrary[String].map(Json.fromString)
  private[this] val genNumber: Gen[Json] = Arbitrary.arbitrary[JsonNumber].map(Json.fromJsonNumber)

  private[this] def genArray(depth: Int): Gen[Json] = Gen.choose(0, maxJsonArraySize).flatMap { size =>
    Gen.listOfN(size, genJsonAtDepth(depth + 1)).map(Json.arr)
  }

  private[this] def genJsonObject(depth: Int): Gen[JsonObject[Json]] = Gen.choose(0, maxJsonObjectSize).flatMap { size =>
    val fields = Gen.listOfN(
      size,
      for {
        key <- Arbitrary.arbitrary[String]
        value <- genJsonAtDepth(depth + 1)
      } yield key -> value
    )

    Gen.oneOf(
      fields.map(JsonObject.fromIterable),
      fields.map(JsonObject.fromFoldable[List, Json])
    )
  }

  private[this] def genJsonAtDepth(depth: Int): Gen[Json] = {
    val genJsons = List(genNumber, genString) ++ (
      if (depth < maxJsonDepth) List(genArray(depth), genJsonObject(depth).map(Json.fromJsonObject)) else Nil
    )

    Gen.oneOf(genNull, genBool, genJsons: _*)
  }

  implicit val arbitraryJson: Arbitrary[Json] = Arbitrary(genJsonAtDepth(0))
  implicit val arbitraryJsonObject: Arbitrary[JsonObject[Json]] = Arbitrary(genJsonObject(0))

  implicit val arbitraryDecodingFailure: Arbitrary[DecodingFailure] = Arbitrary(
    Arbitrary.arbitrary[String].map(DecodingFailure(_, Nil))
  )

  implicit def arbitraryKeyEncoder[A: Cogen]: Arbitrary[KeyEncoder[A]] = Arbitrary(
    Arbitrary.arbitrary[A => String].map(KeyEncoder.instance)
  )

  implicit def arbitraryKeyDecoder[A: Arbitrary]: Arbitrary[KeyDecoder[A]] = Arbitrary(
    Arbitrary.arbitrary[String => Option[A]].map(KeyDecoder.instance)
  )

  implicit def arbitraryEncoder[A: Cogen]: Arbitrary[Encoder[A]] = Arbitrary(
    Arbitrary.arbitrary[A => Json].map(Encoder.instance)
  )

  implicit def arbitraryDecoder[A: Arbitrary]: Arbitrary[Decoder[A]] = Arbitrary(
    Arbitrary.arbitrary[Json => Either[DecodingFailure, A]].map(f => Decoder.instance(c => f(c.value)))
  )

  implicit def arbitraryAsObjectEncoder[A: Cogen]: Arbitrary[Encoder.AsObject[A]] = Arbitrary(
    Arbitrary.arbitrary[A => JsonObject[Json]].map(Encoder.AsObject.instance)
  )

  implicit def arbitraryAsArrayEncoder[A: Cogen, J: Arbitrary]: Arbitrary[Encoder.AsArray[A, J]] = Arbitrary(
    Arbitrary.arbitrary[A => Vector[J]].map(Encoder.AsArray.instance)
  )
}
