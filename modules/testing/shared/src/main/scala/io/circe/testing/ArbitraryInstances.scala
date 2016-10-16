package io.circe.testing

import cats.data.ValidatedNel
import io.circe._
import org.scalacheck.{ Arbitrary, Gen }

trait ArbitraryInstances extends ShrinkInstances {
  private[this] def maxDepth: Int = 5
  private[this] def maxSize: Int = 20

  private[this] def genNull: Gen[Json] = Gen.const(Json.Null)
  private[this] def genBool: Gen[Json] = Arbitrary.arbBool.arbitrary.map(Json.fromBoolean)

  private[this] def genNumber: Gen[Json] = Gen.oneOf(
    Arbitrary.arbLong.arbitrary.map(Json.fromLong),
    Arbitrary.arbDouble.arbitrary.map(Json.fromDoubleOrNull)
  )

  private[this] def genString: Gen[Json] = Arbitrary.arbString.arbitrary.map(Json.fromString)

  private[this] def genArray(depth: Int): Gen[Json] = Gen.choose(0, maxSize).flatMap { size =>
    Gen.listOfN(
      size,
      arbitraryJsonAtDepth(depth + 1).arbitrary
    ).map(Json.arr)
  }

  private[this] def genObject(depth: Int): Gen[Json] = Gen.choose(0, maxSize).flatMap { size =>
    Gen.listOfN(
      size,
      for {
        k <- Arbitrary.arbString.arbitrary
        v <- arbitraryJsonAtDepth(depth + 1).arbitrary
      } yield k -> v
    ).map(Json.obj)
  }

  private[this] def arbitraryJsonAtDepth(depth: Int): Arbitrary[Json] = {
    val genJsons = List(genNumber, genString) ++ (
      if (depth < maxDepth) List(genArray(depth), genObject(depth)) else Nil
    )

    Arbitrary(Gen.oneOf(genNull, genBool, genJsons: _*))
  }

  implicit def arbitraryJson: Arbitrary[Json] = arbitraryJsonAtDepth(0)

  implicit def arbitraryJsonObject: Arbitrary[JsonObject] =
    Arbitrary(genObject(0).map(_.asObject.get))

  implicit def arbitraryJsonNumber: Arbitrary[JsonNumber] = Arbitrary(
    Gen.oneOf(
      Arbitrary.arbitrary[JsonNumberString].map(jns => JsonNumber.unsafeDecimal(jns.value)),
      Arbitrary.arbitrary[BigDecimal].map(JsonBigDecimal(_)),
      Arbitrary.arbitrary[Long].map(JsonLong(_)),
      Arbitrary.arbitrary[Double].map(d => if (d.isNaN || d.isInfinity) JsonDouble(0.0) else JsonDouble(d))
    )
  )

  implicit val arbitraryDecodingFailure: Arbitrary[DecodingFailure] = Arbitrary(
    Arbitrary.arbitrary[String].map(DecodingFailure(_, Nil))
  )

  implicit def arbitraryEncoder[A](implicit arbitraryF: Arbitrary[A => Json]): Arbitrary[Encoder[A]] = Arbitrary(
    arbitraryF.arbitrary.map(Encoder.instance)
  )

  implicit def arbitraryDecoder[A](implicit
    arbitraryF: Arbitrary[Json => Either[DecodingFailure, A]]
  ): Arbitrary[Decoder[A]] = Arbitrary(
    arbitraryF.arbitrary.map(f =>
      Decoder.instance(c => f(c.focus))
    )
  )

  implicit def arbitraryAccumulatingDecoder[A](implicit
    arbitraryF: Arbitrary[Json => ValidatedNel[DecodingFailure, A]]
  ): Arbitrary[AccumulatingDecoder[A]] = Arbitrary(
    arbitraryF.arbitrary.map(f =>
      AccumulatingDecoder.instance(c => f(c.focus))
    )
  )
}
