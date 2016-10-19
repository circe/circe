package io.circe.testing

import cats.data.ValidatedNel
import cats.laws.discipline.arbitrary._
import io.circe._
import io.circe.numbers.BiggerDecimal
import org.scalacheck.{ Arbitrary, Cogen, Gen }
import org.scalacheck.Arbitrary.arbitrary

trait ArbitraryInstances extends ArbitraryJsonNumberTransformer with CogenInstances with ShrinkInstances {
  /**
   * The maximum number of values in a generated JSON array.
   */
  protected def maxJsonArraySize: Int = 10

  /**
   * The maximum depth of a generated JSON object.
   */
  protected def maxJsonObjectDepth: Int = 5

  /**
   * The maximum number of key-value pairs in a generated JSON object.
   */
  protected def maxJsonObjectSize: Int = 10

  implicit val arbitraryBiggerDecimal: Arbitrary[BiggerDecimal] = Arbitrary(
    Gen.oneOf(
      Arbitrary.arbitrary[JsonNumberString].map(s => BiggerDecimal.parseBiggerDecimalUnsafe(s.value)),
      Arbitrary.arbitrary[Long].map(BiggerDecimal.fromLong),
      Arbitrary.arbitrary[Double].map(BiggerDecimal.fromDouble),
      Arbitrary.arbitrary[BigInt].map(_.bigInteger).map(BiggerDecimal.fromBigInteger),
      Arbitrary.arbitrary[BigDecimal].map(_.bigDecimal).map(BiggerDecimal.fromBigDecimal),
      Gen.const(BiggerDecimal.NegativeZero)
    )
  )

  implicit val arbitraryJsonNumber: Arbitrary[JsonNumber] = Arbitrary(
    Gen.oneOf(
      arbitrary[JsonNumberString].map(jns => JsonNumber.fromDecimalStringUnsafe(jns.value)),
      arbitrary[BiggerDecimal].map(JsonBiggerDecimal(_)),
      arbitrary[BigDecimal].map(JsonBigDecimal(_)),
      arbitrary[Long].map(JsonLong(_)),
      arbitrary[Double].map(d => if (d.isNaN || d.isInfinity) JsonDouble(0.0) else JsonDouble(d))
    ).map(transformJsonNumber)
  )

  private[this] val genNull: Gen[Json] = Gen.const(Json.Null)
  private[this] val genBool: Gen[Json] = arbitrary[Boolean].map(Json.fromBoolean)
  private[this] val genString: Gen[Json] = arbitrary[String].map(Json.fromString)
  private[this] val genNumber: Gen[Json] = Arbitrary.arbitrary[JsonNumber].map(Json.fromJsonNumber)

  private[this] def genArray(depth: Int): Gen[Json] = Gen.choose(0, maxJsonArraySize).flatMap { size =>
    Gen.listOfN(
      size,
      arbitraryJsonAtDepth(depth + 1).arbitrary
    ).map(Json.arr)
  }

  private[this] def genObject(depth: Int): Gen[Json] = Gen.choose(0, maxJsonObjectSize).flatMap { size =>
    Gen.listOfN(
      size,
      for {
        k <- arbitrary[String]
        v <- arbitraryJsonAtDepth(depth + 1).arbitrary
      } yield k -> v
    ).map(Json.obj)
  }

  private[this] def arbitraryJsonAtDepth(depth: Int): Arbitrary[Json] = {
    val genJsons = List(genNumber, genString) ++ (
      if (depth < maxJsonObjectDepth) List(genArray(depth), genObject(depth)) else Nil
    )

    Arbitrary(Gen.oneOf(genNull, genBool, genJsons: _*))
  }

  implicit val arbitraryJson: Arbitrary[Json] = arbitraryJsonAtDepth(0)
  implicit val arbitraryJsonObject: Arbitrary[JsonObject] = Arbitrary(genObject(0).map(_.asObject.get))

  implicit val arbitraryDecodingFailure: Arbitrary[DecodingFailure] = Arbitrary(
    arbitrary[String].map(DecodingFailure(_, Nil))
  )

  implicit def arbitraryEncoder[A: Cogen]: Arbitrary[Encoder[A]] = Arbitrary(
    arbitrary[A => Json].map(Encoder.instance)
  )

  implicit def arbitraryDecoder[A: Arbitrary]: Arbitrary[Decoder[A]] = Arbitrary(
    arbitrary[Json => Either[DecodingFailure, A]].map(f =>
      Decoder.instance(c => f(c.focus))
    )
  )

  implicit def arbitraryAccumulatingDecoder[A: Arbitrary]: Arbitrary[AccumulatingDecoder[A]] = Arbitrary(
    //Arbitrary.arbFunction1[Json, ValidatedNel[DecodingFailure, A]].map(f =>
    arbitrary[Json => ValidatedNel[DecodingFailure, A]].map(f =>
      AccumulatingDecoder.instance(c => f(c.focus))
    )
  )
}
