package io.circe.tests

import cats.data.{ ValidatedNel, Xor }
import io.circe._
import io.circe.Json.{ JArray, JNumber, JObject, JString }
import java.util.UUID
import org.scalacheck.{ Arbitrary, Gen, Shrink }

trait ArbitraryInstances {
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

  implicit def arbitraryUUID: Arbitrary[UUID] = Arbitrary(Gen.uuid)

  private[this] val minNumberShrink = BigDecimal.valueOf(1L)
  private[this] val zero = BigDecimal.valueOf(0L)
  private[this] val two = BigDecimal.valueOf(2L)

  /**
   * Copied from ScalaCheck.
   */
  private[this] def interleave[T](xs: Stream[T], ys: Stream[T]): Stream[T] =
    if (xs.isEmpty) ys
    else if (ys.isEmpty) xs
    else xs.head #:: ys.head #:: interleave(xs.tail, ys.tail)

  implicit def shrinkJsonNumber: Shrink[JsonNumber] = Shrink { jn =>
    def halfs(n: BigDecimal): Stream[BigDecimal] =
      if (n < minNumberShrink) Stream.empty else n #:: halfs(n / two)

    jn.toBigDecimal match {
      case Some(n) =>
        val ns = if (n == zero) Stream.empty else {
          val hs = halfs(n / two).map(n - _)
          zero #:: interleave(hs, hs.map(h => -h))
        }

        ns.map(JsonBigDecimal(_))
      case None => Stream(jn)
    }
  }

  implicit def shrinkJsonObject: Shrink[JsonObject] = Shrink(o =>
    Shrink.shrinkContainer[List, (String, Json)].shrink(o.toList).map(JsonObject.fromIterable)
  )

  implicit def shrinkJson: Shrink[Json] = Shrink(
    _.fold(
      Stream.empty,
      _ => Stream.empty,
      n => shrinkJsonNumber.shrink(n).map(JNumber(_)),
      s => Shrink.shrinkString.shrink(s).map(JString(_)),
      a => Shrink.shrinkContainer[List, Json].shrink(a).map(JArray(_)),
      o => shrinkJsonObject.shrink(o).map(JObject(_))
    )
  )

  implicit val arbitraryJsonNumber: Arbitrary[JsonNumber] = Arbitrary(
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
    arbitraryF: Arbitrary[Json => Xor[DecodingFailure, A]]
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
