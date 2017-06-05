package io.circe.testing

import cats.data.ValidatedNel
import cats.laws.discipline.arbitrary._
import io.circe._
import io.circe.numbers.JsonNumber
import io.circe.numbers.testing.JsonNumberString
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

  implicit val arbitraryJsonNumber: Arbitrary[JsonNumber] = Arbitrary(
    Gen.oneOf(
      arbitrary[JsonNumberString].map(jns => JsonNumber.lazyJsonNumberUnsafe(jns.value)),
      arbitrary[JsonNumberString].map(jns => JsonNumber.parseJsonNumberUnsafe(jns.value)),
      arbitrary[BigInt].map(_.bigInteger).map(JsonNumber.fromBigInteger),
      arbitrary[BigDecimal].map(_.bigDecimal).map(JsonNumber.fromBigDecimal),
      arbitrary[Long].map(JsonNumber.fromLong),
      arbitrary[Double].map(d => JsonNumber.fromDouble(if (d.isNaN || d.isInfinity) 0.0 else d)),
      arbitrary[Float].map(f => JsonNumber.fromFloat(if (f.isNaN || f.isInfinity) 0.0f else f)),
      Gen.const(JsonNumber.NegativeZero)
    ).map(transformJsonNumber)
  )

  private[this] val genNull: Gen[Json] = Gen.const(Json.Null)
  private[this] val genBool: Gen[Json] = arbitrary[Boolean].map(Json.fromBoolean)
  private[this] val genString: Gen[Json] = arbitrary[String].map(Json.fromString)
  private[this] val genNumber: Gen[Json] = Gen.oneOf(
    arbitrary[JsonNumber].map(Json.fromJsonNumber),
    arbitrary[Double].map(Json.fromDoubleOrNull),
    arbitrary[Float].map(Json.fromFloatOrNull),
    arbitrary[Long].map(Json.fromLong)
  )

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
      Decoder.instance(c => f(c.value))
    )
  )

  implicit def arbitraryObjectEncoder[A: Cogen]: Arbitrary[ObjectEncoder[A]] = Arbitrary(
    arbitrary[A => JsonObject].map(ObjectEncoder.instance)
  )

  implicit def arbitraryArrayEncoder[A: Cogen]: Arbitrary[ArrayEncoder[A]] = Arbitrary(
    arbitrary[A => Vector[Json]].map(ArrayEncoder.instance)
  )

  implicit def arbitraryAccumulatingDecoder[A: Arbitrary]: Arbitrary[AccumulatingDecoder[A]] = Arbitrary(
    arbitrary[Json => ValidatedNel[DecodingFailure, A]].map(f =>
      AccumulatingDecoder.instance(c => f(c.value))
    )
  )
}
