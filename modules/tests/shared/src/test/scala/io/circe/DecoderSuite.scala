package io.circe

import cats.laws.discipline.{ MonadErrorTests, SemigroupKTests }
import io.circe.parser.parse
import io.circe.syntax._
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import io.circe.tests.examples.WrappedOptionalField
import scala.util.{ Failure, Success, Try }
import scala.util.control.NoStackTrace

class DecoderSuite extends CirceSuite with LargeNumberDecoderTests {
  checkLaws("Decoder[Int]", MonadErrorTests[Decoder, DecodingFailure].monadError[Int, Int, Int])
  checkLaws("Decoder[Int]", SemigroupKTests[Decoder].semigroupK[Int])

  "prepare" should "do nothing when used with ok" in forAll { (i: Int) =>
    assert(Decoder[Int].prepare(ACursor.ok).decodeJson(i.asJson) === Right(i))
  }

  it should "move appropriately with downField" in forAll { (i: Int, k: String, m: Map[String, Int]) =>
    assert(Decoder[Int].prepare(_.downField(k)).decodeJson(m.updated(k, i).asJson) === Right(i))
  }

  "emap" should "do nothing when used with right" in forAll { (i: Int) =>
    assert(Decoder[Int].emap(Right(_)).decodeJson(i.asJson) === Right(i))
  }

  it should "appropriately transform the result with an operation that can't fail" in forAll { (i: Int) =>
    assert(Decoder[Int].emap(v => Right(v + 1)).decodeJson(i.asJson) === Right(i + 1))
  }

  it should "appropriately transform the result with an operation that may fail" in forAll { (i: Int) =>
    val decoder = Decoder[Int].emap(v => if (v % 2 == 0) Right(v) else Left("Odd"))
    val expected = if (i % 2 == 0) Right(i) else Left(DecodingFailure("Odd", Nil))

    assert(decoder.decodeJson(i.asJson) === expected)
  }

  "emapTry" should "do nothing when used with Success" in forAll { (i: Int) =>
    assert(Decoder[Int].emapTry(Success(_)).decodeJson(i.asJson) === Right(i))
  }

  it should "appropriately transform the result with an operation that can't fail" in forAll { (i: Int) =>
    assert(Decoder[Int].emapTry(v => Success(v + 1)).decodeJson(i.asJson) === Right(i + 1))
  }

  it should "appropriately transform the result with an operation that may fail" in forAll { (i: Int) =>
    val exception = new Exception("Odd") with NoStackTrace
    val decoder = Decoder[Int].emapTry(v => if (v % 2 == 0) Success(v) else Failure(exception))

    assert(decoder.decodeJson(i.asJson).isRight == (i % 2 == 0))
  }

  "failedWithMessage" should "replace the message" in forAll { (json: Json) =>
    assert(Decoder.failedWithMessage[Int]("Bad").decodeJson(json) === Left(DecodingFailure("Bad", Nil)))
  }

  "An optional object field decoder" should "fail appropriately" in {
    val decoder: Decoder[Option[String]] = Decoder.instance(
      _.downField("").downField("").as[Option[String]]
    )

    forAll { (json: Json) =>
      val result = decoder.apply(json.hcursor)

      assert(
        json.asObject match {
          // The top-level value isn't an object, so we should fail.
          case None => result.isLeft
          case Some(o1) => o1("") match {
            // The top-level object doesn't contain a "" key, so we should succeed emptily.
            case None => result === Right(None)
            case Some(j2) => j2.asObject match {
              // The second-level value isn't an object, so we should fail.
              case None => result.isLeft
              case Some(o2) => o2("") match {
                // The second-level object doesn't contain a "" key, so we should succeed emptily.
                case None => result === Right(None)
                // The third-level value is null, so we succeed emptily.
                case Some(j3) if j3.isNull => result === Right(None)
                case Some(j3) => j3.asString match {
                  // The third-level value isn't a string, so we should fail.
                  case None => result.isLeft
                  // The third-level value is a string, so we should have decoded it.
                  case Some(s3) => result === Right(Some(s3))
                }
              }
            }
          }
        }
      )
    }
  }

  "An optional array position decoder" should "fail appropriately" in {
    val decoder: Decoder[Option[String]] = Decoder.instance(
      _.downN(0).downN(1).as[Option[String]]
    )

    forAll { (json: Json) =>
      val result = decoder.apply(json.hcursor)

      assert(
        json.asArray match {
          // The top-level value isn't an array, so we should fail.
          case None => result.isLeft
          case Some(a1) => a1.lift(0) match {
            // The top-level array is empty, so we should succeed emptily.
            case None => result === Right(None)
            case Some(j2) => j2.asArray match {
              // The second-level value isn't an array, so we should fail.
              case None => result.isLeft
              case Some(a2) => a2.lift(1) match {
                // The second-level array doesn't have a second element, so we should succeed emptily.
                case None => result === Right(None)
                // The third-level value is null, so we succeed emptily.
                case Some(j3) if j3.isNull => result === Right(None)
                case Some(j3) => j3.asString match {
                  // The third-level value isn't a string, so we should fail.
                  case None => result.isLeft
                  // The third-level value is a string, so we should have decoded it.
                  case Some(s3) => result === Right(Some(s3))
                }
              }
            }
          }
        }
      )
    }
  }

  "instanceTry" should "provide instances that succeed or fail appropriately" in forAll { (json: Json) =>
    val exception = new Exception("Not an Int")
    val expected = json.hcursor.as[Int].leftMap(_ => DecodingFailure.fromThrowable(exception, Nil))
    val instance = Decoder.instanceTry(c => Try(c.as[Int].right.getOrElse(throw exception)))

    assert(instance.decodeJson(json) === expected)
  }

  "Decoder[Byte]" should "fail on out-of-range values (#83)" in forAll { (l: Long) =>
    val json = Json.fromLong(l)
    val result = Decoder[Byte].apply(json.hcursor)

    assert(if (l.toByte.toLong == l) result === Right(l.toByte) else result.isEmpty)
  }

  it should "fail on non-whole values (#83)" in forAll { (d: Double) =>
    val json = Json.fromDoubleOrNull(d)
    val result = Decoder[Byte].apply(json.hcursor)

    assert(d.isWhole || result.isEmpty)
  }

  it should "succeed on whole decimal values (#83)" in forAll { (v: Byte, n: Byte) =>
    val zeros = "0" * (math.abs(n.toInt) + 1)
    val Right(json) = parse(s"$v.$zeros")

    assert(Decoder[Byte].apply(json.hcursor) === Right(v))
  }

  "Decoder[Short]" should "fail on out-of-range values (#83)" in forAll { (l: Long) =>
    val json = Json.fromLong(l)
    val result = Decoder[Short].apply(json.hcursor)

    assert(if (l.toShort.toLong == l) result === Right(l.toShort) else result.isEmpty)
  }

  it should "fail on non-whole values (#83)" in forAll { (d: Double) =>
    val json = Json.fromDoubleOrNull(d)
    val result = Decoder[Short].apply(json.hcursor)

    assert(d.isWhole || result.isEmpty)
  }

  it should "succeed on whole decimal values (#83)" in forAll { (v: Short, n: Byte) =>
    val zeros = "0" * (math.abs(n.toInt) + 1)
    val Right(json) = parse(s"$v.$zeros")

    assert(Decoder[Short].apply(json.hcursor) === Right(v))
  }

  "Decoder[Int]" should "fail on out-of-range values (#83)" in forAll { (l: Long) =>
    val json = Json.fromLong(l)
    val result = Decoder[Int].apply(json.hcursor)

    assert(if (l.toInt.toLong == l) result === Right(l.toInt) else result.isEmpty)
  }

  it should "fail on non-whole values (#83)" in forAll {(d: Double) =>
    val json = Json.fromDoubleOrNull(d)
    val result = Decoder[Int].apply(json.hcursor)

    assert(d.isWhole || result.isEmpty)
  }

  it should "succeed on whole decimal values (#83)" in forAll { (v: Int, n: Byte) =>
    val zeros = "0" * (math.abs(n.toInt) + 1)
    val Right(json) = parse(s"$v.$zeros")

    assert(Decoder[Int].apply(json.hcursor) === Right(v))
  }

  "Decoder[Long]" should "fail on out-of-range values (#83)" in forAll { (i: BigInt) =>
    val json = Json.fromBigDecimal(BigDecimal(i))
    val result = Decoder[Long].apply(json.hcursor)

    assert(if (BigInt(i.toLong) == i) result === Right(i.toLong) else result.isEmpty)
  }

  it should "fail on non-whole values (#83)" in forAll { (d: Double) =>
    val json = Json.fromDoubleOrNull(d)
    val result = Decoder[Long].apply(json.hcursor)

    assert(d.isWhole || result.isEmpty)
  }

  "Decoder[Float]" should "attempt to parse string values as doubles (#173)" in forAll { (d: Float) =>
    val Right(json) = parse("\"" + d.toString + "\"")

    assert(Decoder[Float].apply(json.hcursor) === Right(d))
  }

  "Decoder[Double]" should "attempt to parse string values as doubles (#173)" in forAll { (d: Double) =>
    val Right(json) = parse("\"" + d.toString + "\"")

    assert(Decoder[Double].apply(json.hcursor) === Right(d))
  }

  "Decoder[BigInt]" should "fail when producing a value would be intractable" in {
    val Right(bigNumber) = parse("1e2147483647")

    assert(Decoder[BigInt].apply(bigNumber.hcursor).isEmpty)
  }

  "Decoder[Enumeration]" should "parse Scala Enumerations" in {
    object WeekDay extends Enumeration {
      type WeekDay = Value
      val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
    }

    val decoder = Decoder.enumDecoder(WeekDay)
    val Right(friday) = parse("\"Fri\"")
    assert(decoder.apply(friday.hcursor) == Right(WeekDay.Fri))
  }

  "Decoder[Enumeration]" should "fail on unknown values in Scala Enumerations" in {
    object WeekDay extends Enumeration {
      type WeekDay = Value
      val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
    }

    val decoder = Decoder.enumDecoder(WeekDay)
    val Right(friday) = parse("\"Friday\"")

    assert(decoder.apply(friday.hcursor).isEmpty)
  }

  "validate" should "not infinitely recurse (#396)" in forAll { (i: Int) =>
    assert(Decoder[Int].validate(_ => true, "whatever").apply(Json.fromInt(i).hcursor) === Right(i))
  }

  private[this] val stateful = {
    import Decoder.state._
    Decoder.fromState(for {
      a <- decodeField[String]("a")
      b <- decodeField[String]("b")
      _ <- requireEmpty
    } yield a ++ b)
  }

  "a stateful Decoder with requireEmpty" should "succeed when there are no leftover fields" in {
    val json = Json.obj(
      "a" -> "1".asJson,
      "b" -> "2".asJson)

    assert(stateful.decodeJson(json) === Right("12"))
  }

  it should "fail when there are leftover fields" in {
    val json = Json.obj(
      "a" -> "1".asJson,
      "b" -> "2".asJson,
      "c" -> "3".asJson,
      "d" -> "4".asJson)

    assert(stateful.decodeJson(json).left.get.message === "Leftover keys: c, d")
  }

  it should "fail normally when a field is missing" in {
    val json = Json.obj(
      "a" -> "1".asJson)

    assert(stateful.decodeJson(json).left.get.message === "Attempt to decode value on failed cursor")
  }

  checkLaws("Codec[WrappedOptionalField]", CodecTests[WrappedOptionalField].codec)

  "decodeSet" should "match sequence decoders" in forAll { (xs: List[Int]) =>
    assert(Decoder.decodeSet[Int].decodeJson(xs.asJson) === Decoder[Seq[Int]].map(_.toSet).decodeJson(xs.asJson))
  }

  "decodeList" should "match sequence decoders" in forAll { (xs: List[Int]) =>
    assert(Decoder.decodeList[Int].decodeJson(xs.asJson) === Decoder[Seq[Int]].map(_.toList).decodeJson(xs.asJson))
  }

  "decodeVector" should "match sequence decoders" in forAll { (xs: List[Int]) =>
    assert(Decoder.decodeVector[Int].decodeJson(xs.asJson) === Decoder[Seq[Int]].map(_.toVector).decodeJson(xs.asJson))
  }
}
