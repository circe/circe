package io.circe

import cats.data.Validated.Invalid
import cats.data.{ Chain, NonEmptyList, Validated }
import cats.implicits._
import cats.kernel.Eq
import cats.laws.discipline.{ MonadErrorTests, SemigroupKTests }
import io.circe.CursorOp.{ DownArray, DownN }
import io.circe.parser.parse
import io.circe.syntax._
import io.circe.testing.CodecTests
import io.circe.tests.CirceMunitSuite
import io.circe.tests.examples.WrappedOptionalField
import org.scalacheck.Prop.forAll
import scala.util.{ Failure, Success, Try }
import scala.util.control.NoStackTrace

class DecoderSuite extends CirceMunitSuite with LargeNumberDecoderTests {
  checkAll("Decoder[Int]", MonadErrorTests[Decoder, DecodingFailure].monadError[Int, Int, Int])
  checkAll("Decoder[Int]", SemigroupKTests[Decoder].semigroupK[Int])

  private[this] def transformations[T]: List[Decoder[T] => Decoder[T]] = List(
    _.prepare(identity),
    _.map(identity),
    _.emap(Right(_)),
    _.emapTry(Success(_))
  )

  private[this] def containerDecoders[T: Decoder]: List[Decoder[_]] = List(
    Decoder[Set[T]],
    Decoder[List[T]],
    Decoder[Vector[T]],
    Decoder[Chain[T]]
  )

  property("transformations should do nothing when used with identity") {
    transformations[Int].foreach { transformation =>
      val decoder = transformation(Decoder[Int])
      forAll { (i: Int) =>
        assertEquals(decoder.decodeJson(i.asJson), Right(i))
        assertEquals(decoder.decodeAccumulating(i.asJson.hcursor), Validated.valid(i))
      }
    }
  }

  property("transformations should fail when called on failed decoder") {
    transformations[Int].foreach { transformation =>
      val decoder = transformation(Decoder.failedWithMessage("Some message"))
      val failure = DecodingFailure("Some message", Nil)
      forAll { (i: Int) =>
        assertEquals(decoder.decodeJson(i.asJson), Left(failure))
        assertEquals(decoder.decodeAccumulating(i.asJson.hcursor), Validated.invalidNel(failure))
      }
    }
  }

  property("transformations should not break derived decoders when called on Decoder[Option[T]]") {
    transformations[Option[String]].foreach { transformation =>
      implicit val decodeOptionString: Decoder[Option[String]] =
        transformation(Decoder.decodeOption(Decoder.decodeString))

      object Test {
        implicit val eqTest: Eq[Test] = Eq.fromUniversalEquals
        implicit val decodeTest: Decoder[Test] = Decoder.forProduct1("a")(Test.apply)
      }

      case class Test(a: Option[String])
      // Dotty crashes here with `CyclicReference` on `assert`.
      assertEquals(Decoder[Test].decodeJson(Json.obj()), Right(Test(None)))
      assertEquals(Decoder[Test].decodeAccumulating(Json.obj().hcursor), Validated.valid(Test(None)))
    }
  }

  property("prepare should move appropriately with downField") {
    forAll { (i: Int, k: String, m: Map[String, Int]) =>
      assertEquals(Decoder[Int].prepare(_.downField(k)).decodeJson(m.updated(k, i).asJson), Right(i))
    }
  }

  property("at should move appropriately") {
    forAll { (i: Int, k: String, m: Map[String, Int]) =>
      assertEquals(Decoder[Int].at(k).decodeJson(m.updated(k, i).asJson), Right(i))
    }
  }

  property("at should accumulate errors") {
    forAll { (k: String, x: Boolean, xs: List[Boolean], m: Map[String, Int]) =>
      val json = m.mapValues(_.asJson).toMap.updated(k, (x :: xs).asJson).asJson
      val actual = Decoder[List[Int]].at(k).decodeAccumulating(json.hcursor).leftMap(_.size)
      assertEquals(actual, Validated.invalid(xs.size + 1))
    }
  }

  property("emap should appropriately transform the result with an operation that can't fail") {
    forAll { (i: Int) =>
      assertEquals(Decoder[Int].emap(v => Right(v + 1)).decodeJson(i.asJson), Right(i + 1))
    }
  }

  property("emap should appropriately transform the result with an operation that may fail") {
    forAll { (i: Int) =>
      val decoder = Decoder[Int].emap(v => if (v % 2 == 0) Right(v) else Left("Odd"))
      val expected = if (i % 2 == 0) Right(i) else Left(DecodingFailure("Odd", Nil))

      assertEquals(decoder.decodeJson(i.asJson), expected)
    }
  }

  property("emapTry should appropriately transform the result with an operation that can't fail") {
    forAll { (i: Int) =>
      assertEquals(Decoder[Int].emapTry(v => Success(v + 1)).decodeJson(i.asJson), Right(i + 1))
    }
  }

  property("emapTry should appropriately transform the result with an operation that may fail") {
    forAll { (i: Int) =>
      val exception = new Exception("Odd") with NoStackTrace
      val decoder = Decoder[Int].emapTry(v => if (v % 2 == 0) Success(v) else Failure(exception))

      assertEquals(decoder.decodeJson(i.asJson).isRight, i % 2 == 0)
    }
  }

  test("handleErrorWith should respect the underlying decoder's tryDecode (#1271)") {
    val decoder: Decoder[Option[String]] =
      Decoder.decodeOption[String].handleErrorWith(_ => Decoder.const(None)).at("a")

    assertEquals(decoder.decodeJson(Json.obj("a" := 1)), Right(None))
    assertEquals(decoder.decodeJson(Json.obj("a" := Json.Null)), Right(None))
    assertEquals(decoder.decodeJson(Json.obj("b" := "abc")), Right(None))
    assertEquals(decoder.decodeJson(Json.obj("a" := "abc")), Right(Some("abc")))

    assertEquals(decoder.decodeAccumulating(Json.obj("a" := 1).hcursor), Validated.valid(None))
    assertEquals(decoder.decodeAccumulating(Json.obj("a" := Json.Null).hcursor), Validated.valid(None))
    assertEquals(decoder.decodeAccumulating(Json.obj("b" := "abc").hcursor), Validated.valid(None))
    assertEquals(decoder.decodeAccumulating(Json.obj("a" := "abc").hcursor), Validated.valid(Some("abc")))
  }

  property("failedWithMessage should replace the message") {
    forAll { (json: Json) =>
      assertEquals(Decoder.failedWithMessage[Int]("Bad").decodeJson(json), Left(DecodingFailure("Bad", Nil)))
    }
  }

  property("An optional object field decoder should fail appropriately") {
    val decoder: Decoder[Option[String]] = Decoder.instance(
      _.downField("").downField("").as[Option[String]]
    )

    forAll { (json: Json) =>
      val result = decoder.apply(json.hcursor)

      json.asObject match {
        // The top-level value isn't an object, so we should fail.
        case None => assert(result.isLeft)
        case Some(o1) =>
          o1("") match {
            // The top-level object doesn't contain a "" key, so we should succeed emptily.
            case None => assertEquals(result, Right(None))
            case Some(j2) =>
              j2.asObject match {
                // The second-level value isn't an object, so we should fail.
                case None => assert(result.isLeft)
                case Some(o2) =>
                  o2("") match {
                    // The second-level object doesn't contain a "" key, so we should succeed emptily.
                    case None => assertEquals(result, Right(None))
                    // The third-level value is null, so we succeed emptily.
                    case Some(j3) if j3.isNull => assertEquals(result, Right(None))
                    case Some(j3) =>
                      j3.asString match {
                        // The third-level value isn't a string, so we should fail.
                        case None => assert(result.isLeft)
                        // The third-level value is a string, so we should have decoded it.
                        case Some(s3) => assertEquals(result, Right(Some(s3)))
                      }
                  }
              }
          }
      }
    }
  }

  property("An optional array position decoder should fail appropriately") {
    val decoder: Decoder[Option[String]] = Decoder.instance(
      _.downN(0).downN(1).as[Option[String]]
    )

    forAll { (json: Json) =>
      val result = decoder.apply(json.hcursor)

      json.asArray match {
        // The top-level value isn't an array, so we should fail.
        case None => assert(result.isLeft)
        case Some(a1) =>
          a1.lift(0) match {
            // The top-level array is empty, so we should succeed emptily.
            case None => assertEquals(result, Right(None))
            case Some(j2) =>
              j2.asArray match {
                // The second-level value isn't an array, so we should fail.
                case None => assert(result.isLeft)
                case Some(a2) =>
                  a2.lift(1) match {
                    // The second-level array doesn't have a second element, so we should succeed emptily.
                    case None => assertEquals(result, Right(None))
                    // The third-level value is null, so we succeed emptily.
                    case Some(j3) if j3.isNull => assertEquals(result, Right(None))
                    case Some(j3) =>
                      j3.asString match {
                        // The third-level value isn't a string, so we should fail.
                        case None => assert(result.isLeft)
                        // The third-level value is a string, so we should have decoded it.
                        case Some(s3) => assertEquals(result, Right(Some(s3)))
                      }
                  }
              }
          }
      }
    }
  }

  property("An optional top-level decoder should fail appropriately") {
    val decoder: Decoder[Option[String]] = Decoder.instance(_.as[Option[String]])

    forAll { (json: Json) =>
      val result = decoder.apply(json.hcursor)
      if (json.isNull)
        assertEquals(result, Right(None))
      else
        json.asString match {
          case Some(str) => assertEquals(result, Right(Some(str)))
          case None      => assert(result.isLeft)
        }
    }
  }

  test("A nested optional decoder should accumulate failures") {
    val pair = Json.arr(Json.fromInt(1), Json.fromInt(2))

    val result = Decoder[Option[(String, String)]].decodeAccumulating(pair.hcursor)
    val expected = Validated.invalid(
      NonEmptyList.of(DecodingFailure("String", List(DownN(0))), DecodingFailure("String", List(DownN(1))))
    )
    assertEquals(result, expected)
  }

  property("instanceTry should provide instances that succeed or fail appropriately") {
    forAll { (json: Json) =>
      val exception = new Exception("Not an Int")
      val expected = json.hcursor.as[Int].leftMap(_ => DecodingFailure.fromThrowable(exception, Nil))
      val instance = Decoder.instanceTry(c => Try(c.as[Int].getOrElse(throw exception)))

      assertEquals(instance.decodeJson(json), expected)
    }
  }

  group("Decoder[Byte] should") {
    property("fail on out-of-range values (#83)") {
      forAll { (l: Long) =>
        val json = Json.fromLong(l)
        val result = Decoder[Byte].apply(json.hcursor)

        assert(if (l.toByte.toLong == l) result === Right(l.toByte) else result.isLeft)
      }
    }

    property("fail on non-whole values (#83)") {
      forAll { (d: Double) =>
        val json = Json.fromDoubleOrNull(d)
        val result = Decoder[Byte].apply(json.hcursor)

        assert(d.isWhole || result.isLeft)
      }
    }

    property("succeed on whole decimal values (#83)") {
      forAll { (v: Byte, n: Byte) =>
        val zeros = "0" * (math.abs(n.toInt) + 1)
        val Right(json) = parse(s"$v.$zeros")

        assertEquals(Decoder[Byte].apply(json.hcursor), Right(v))
      }
    }
  }

  group("Decoder[Short] should") {
    property("fail on out-of-range values (#83)") {
      forAll { (l: Long) =>
        val json = Json.fromLong(l)
        val result = Decoder[Short].apply(json.hcursor)

        assert(if (l.toShort.toLong == l) result === Right(l.toShort) else result.isLeft)
      }
    }

    property("fail on non-whole values (#83)") {
      forAll { (d: Double) =>
        val json = Json.fromDoubleOrNull(d)
        val result = Decoder[Short].apply(json.hcursor)

        assert(d.isWhole || result.isLeft)
      }
    }

    property("succeed on whole decimal values (#83)") {
      forAll { (v: Short, n: Byte) =>
        val zeros = "0" * (math.abs(n.toInt) + 1)
        val Right(json) = parse(s"$v.$zeros")

        assertEquals(Decoder[Short].apply(json.hcursor), Right(v))
      }
    }
  }

  group("Decoder[Int] should") {
    property("fail on out-of-range values (#83)") {
      forAll { (l: Long) =>
        val json = Json.fromLong(l)
        val result = Decoder[Int].apply(json.hcursor)

        assert(if (l.toInt.toLong == l) result === Right(l.toInt) else result.isLeft)
      }
    }

    property("fail on non-whole values (#83)") {
      forAll { (d: Double) =>
        val json = Json.fromDoubleOrNull(d)
        val result = Decoder[Int].apply(json.hcursor)

        assert(d.isWhole || result.isLeft)
      }
    }

    property("succeed on whole decimal values (#83)") {
      forAll { (v: Int, n: Byte) =>
        val zeros = "0" * (math.abs(n.toInt) + 1)
        val Right(json) = parse(s"$v.$zeros")

        assertEquals(Decoder[Int].apply(json.hcursor), Right(v))
      }
    }

  }

  property("Decoder[Long] should fail on out-of-range values (#83)") {
    forAll { (i: BigInt) =>
      val json = Json.fromBigDecimal(BigDecimal(i))
      val result = Decoder[Long].apply(json.hcursor)

      if (BigInt(i.toLong) == i) assertEquals(result, Right(i.toLong)) else assert(result.isLeft)
    }
  }

  property("Decoder[Long] should fail on non-whole values (#83)") {
    forAll { (d: Double) =>
      val json = Json.fromDoubleOrNull(d)
      val result = Decoder[Long].apply(json.hcursor)

      assert(d.isWhole || result.isLeft)
    }
  }

  group("Decoder[Float]") {
    property("should attempt to parse string values as doubles (#173)") {
      forAll { (d: Float) =>
        val Right(json) = parse("\"" + d.toString + "\"")

        assertEquals(Decoder[Float].apply(json.hcursor), Right(d))
      }
    }

    property("match the rounding of Float.parseFloat (#1063)") {
      forAll { (d: Double) =>
        val Right(json) = parse(d.toString)

        assertEquals(Decoder[Float].apply(json.hcursor), Right(java.lang.Float.parseFloat(d.toString)))
      }
    }

    test(" should match the rounding of Float.parseFloat for known problematic inputs (#1063)") {
      val bad1 = "1.199999988079071"
      val bad2 = "7.038531E-26"

      val Right(json1) = parse(bad1)
      val Right(json2) = parse(bad2)

      assertEquals(Decoder[Float].apply(json1.hcursor), Right(java.lang.Float.parseFloat(bad1)))
      assertEquals(Decoder[Float].apply(json2.hcursor), Right(java.lang.Float.parseFloat(bad2)))
    }
  }

  property("Decoder[Double] should attempt to parse string values as doubles (#173)") {
    forAll { (d: Double) =>
      val Right(json) = parse("\"" + d.toString + "\"")

      assertEquals(Decoder[Double].apply(json.hcursor), Right(d))
    }
  }

  test("Decoder[BigInt] should fail when producing a value would be intractable") {
    val Right(bigNumber) = parse("1e2147483647")

    assert(Decoder[BigInt].apply(bigNumber.hcursor).isLeft)
  }

  val isPositive: Int => Boolean = _ > 0
  val isOdd: Int => Boolean = _ % 2 != 0

  group("ensure should") {

    property("fail appropriately on an invalid result") {
      forAll { (i: Int) =>
        val message = "Not positive!"

        val decodePositiveInt: Decoder[Int] = Decoder[Int].ensure(_ > 0, message)
        val expected = if (i > 0) Right(i) else Left(DecodingFailure(message, Nil))

        assertEquals(decodePositiveInt.decodeJson(Json.fromInt(i)), expected)
      }
    }

    property("only include the first failure when chained, even in error-accumulation mode") {
      forAll { (i: Int) =>
        val positiveMessage = "Not positive!"
        val oddMessage = "Not odd!"

        val badDecodePositiveOddInt: Decoder[Int] =
          Decoder[Int].ensure(isPositive, positiveMessage).ensure(isOdd, oddMessage)

        val expected = if (isPositive(i)) {
          if (isOdd(i)) {
            Validated.valid(i)
          } else {
            Validated.invalidNel(DecodingFailure(oddMessage, Nil))
          }
        } else {
          Validated.invalidNel(DecodingFailure(positiveMessage, Nil))
        }

        assertEquals(badDecodePositiveOddInt.decodeAccumulating(Json.fromInt(i).hcursor), expected)
      }
    }

    test("not include failures it hasn't checked for") {
      val decodePositiveInt: Decoder[Int] = Decoder[Int].ensure(isPositive, "Not positive!")

      val expected = Validated.invalidNel(DecodingFailure("Int", Nil))

      assertEquals(decodePositiveInt.decodeAccumulating(Json.Null.hcursor), expected)
    }

    property("include all given failures in error-accumulation mode") {
      forAll { (i: Int) =>
        val positiveMessage = "Not positive!"
        val oddMessage = "Not odd!"

        val decodePositiveOddInt: Decoder[Int] =
          Decoder[Int].ensure(i =>
            (if (isPositive(i)) Nil else List(positiveMessage)) ++
              (if (isOdd(i)) Nil else List(oddMessage))
          )

        val expected = if (isPositive(i)) {
          if (isOdd(i)) {
            Validated.valid(i)
          } else {
            Validated.invalidNel(DecodingFailure(oddMessage, Nil))
          }
        } else {
          if (isOdd(i)) {
            Validated.invalidNel(DecodingFailure(positiveMessage, Nil))
          } else {
            Validated.invalid(NonEmptyList.of(DecodingFailure(positiveMessage, Nil), DecodingFailure(oddMessage, Nil)))
          }
        }

        assertEquals(decodePositiveOddInt.decodeAccumulating(Json.fromInt(i).hcursor), expected)
      }
    }
  }

  group("validate should") {
    property("validate should fail appropriately on invalid input in fail-fast mode") {
      forAll { (i: Int) =>
        val message = "Not positive!"
        val decodePositiveInt: Decoder[Int] = Decoder[Int].validate(_.as[Int].exists(_ > 0), message)
        val expected = if (i > 0) Right(i) else Left(DecodingFailure(message, Nil))

        assertEquals(decodePositiveInt.decodeJson(Json.fromInt(i)), expected)
      }
    }

    property("fail appropriately on invalid input in error-accumulation mode (#865)") {
      forAll { (i: Int) =>
        val message = "Not positive!"

        val decodePositiveInt: Decoder[Int] = Decoder[Int].validate(_.as[Int].exists(_ > 0), message)

        val expected = if (i > 0) Validated.valid(i) else Validated.invalidNel(DecodingFailure(message, Nil))

        assertEquals(decodePositiveInt.decodeAccumulating(Json.fromInt(i).hcursor), expected)
      }
    }

    property("not infinitely recurse (#396)") {
      forAll { (i: Int) =>
        assertEquals(Decoder[Int].validate(_ => true, "whatever").apply(Json.fromInt(i).hcursor), Right(i))
      }
    }

    test("preserve error accumulation when validation succeeds") {
      val message = "This shouldn't work"

      trait Foo

      val decoder: Decoder[Foo] = new Decoder[Foo] {
        override def apply(c: HCursor): Decoder.Result[Foo] = Right(new Foo {})

        override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[Foo] = Invalid(
          NonEmptyList.one(DecodingFailure(message, c.history))
        )
      }

      val validatingDecoder = decoder.validate(c => true, "Foobar")

      assert(validatingDecoder.decodeAccumulating(Json.True.hcursor).isInvalid)
    }

    test("provide the generated error messages from HCursor when a function is passed") {
      case class Foo(x: Int, y: String)

      val decoder: Decoder[Foo] = Decoder.const(Foo(42, "meaning")).validate { c =>
        val maybeFieldsStr = for {
          json <- c.focus
          jsonKeys <- json.hcursor.keys
        } yield jsonKeys.mkString(",")
        maybeFieldsStr.getOrElse("") :: Nil
      }

      val Right(fooJson) = parse("""{"x":42, "y": "meaning"}""")

      assert(decoder.decodeJson(fooJson).swap.exists(_.message === "x,y"))
    }

    test("not fail when the passed errors function returns an empty list") {
      val testValue = 42
      val decoder = Decoder[Int].validate(_ => Nil)

      val Right(intJson) = parse(testValue.toString)

      assertEquals(decoder.decodeJson(intJson), Right(testValue))
    }

  }

  property("either should return the correct disjunct") {
    forAll { (value: Either[String, Boolean]) =>
      val json = value match {
        case Left(s)  => Json.fromString(s)
        case Right(b) => Json.fromBoolean(b)
      }

      assertEquals(Decoder[String].either(Decoder[Boolean]).decodeJson(json), Right(value))
    }
  }

  test("either should respect the underlying decoder's tryDecode (#1271)") {
    val decoder: Decoder[Either[Option[String], Boolean]] =
      Decoder.decodeOption[String].either(Decoder.const(true)).at("a")

    assertEquals(decoder.decodeJson(Json.obj("a" := 1)), Right(Right(true)))
    assertEquals(decoder.decodeJson(Json.obj("a" := Json.Null)), Right(Left(None)))
    assertEquals(decoder.decodeJson(Json.obj("b" := "abc")), Right(Left(None)))
    assertEquals(decoder.decodeJson(Json.obj("a" := "abc")), Right(Left(Some("abc"))))

    assertEquals(decoder.decodeAccumulating(Json.obj("a" := 1).hcursor), Validated.valid(Right(true)))
    assertEquals(decoder.decodeAccumulating(Json.obj("a" := Json.Null).hcursor), Validated.valid(Left(None)))
    assertEquals(decoder.decodeAccumulating(Json.obj("b" := "abc").hcursor), Validated.valid(Left(None)))
    assertEquals(decoder.decodeAccumulating(Json.obj("a" := "abc").hcursor), Validated.valid(Left(Some("abc"))))
  }

  test("or should respect the underlying decoder's tryDecode (#1271)") {
    val decoder: Decoder[Option[String]] =
      Decoder.decodeOption[String].or(Decoder.const(Option.empty[String])).at("a")

    assertEquals(decoder.decodeJson(Json.obj("a" := 1)), Right(None))
    assertEquals(decoder.decodeJson(Json.obj("a" := Json.Null)), Right(None))
    assertEquals(decoder.decodeJson(Json.obj("b" := "abc")), Right(None))
    assertEquals(decoder.decodeJson(Json.obj("a" := "abc")), Right(Some("abc")))

    assertEquals(decoder.decodeAccumulating(Json.obj("a" := 1).hcursor), Validated.valid(None))
    assertEquals(decoder.decodeAccumulating(Json.obj("a" := Json.Null).hcursor), Validated.valid(None))
    assertEquals(decoder.decodeAccumulating(Json.obj("b" := "abc").hcursor), Validated.valid(None))
    assertEquals(decoder.decodeAccumulating(Json.obj("a" := "abc").hcursor), Validated.valid(Some("abc")))
  }

  group("a stateful Decoder with requireEmpty should ") {
    val stateful = {
      import Decoder.state._
      Decoder.fromState(for {
        a <- decodeField[String]("a")
        b <- decodeField[String]("b")
        _ <- requireEmpty
      } yield a ++ b)
    }

    test("succeed when there are no leftover fields") {
      val json = Json.obj("a" -> "1".asJson, "b" -> "2".asJson)

      assertEquals(stateful.decodeJson(json), Right("12"))
    }

    test("fail when there are leftover fields") {
      val json = Json.obj("a" -> "1".asJson, "b" -> "2".asJson, "c" -> "3".asJson, "d" -> "4".asJson)

      assert(stateful.decodeJson(json).swap.exists(_.message === "Leftover keys: c, d"))
    }

    test("fail normally when a field is missing") {
      val json = Json.obj("a" -> "1".asJson)

      assert(stateful.decodeJson(json).swap.exists(_.message === "Attempt to decode value on failed cursor"))
    }
  }

  group("a stateful Decoder with requireEmpty and an optional value should") {

    val statefulOpt = {
      import Decoder.state._
      Decoder.fromState(for {
        a <- decodeField[Option[String]]("a")
        b <- decodeField[String]("b")
        _ <- requireEmpty
      } yield a.foldMap(identity) ++ b)
    }

    test("succeed when there are no leftover fields and an optional field is missing") {
      val json = Json.obj("b" -> "2".asJson)

      assertEquals(statefulOpt.decodeJson(json), Right("2"))
    }

    test("succeed when there are no leftover fields and an optional field is present") {
      val json = Json.obj("a" -> "1".asJson, "b" -> "2".asJson)
      assertEquals(statefulOpt.decodeJson(json), Right("12"))
    }

    test("fail when there are leftover fields and an optional field is missing") {
      val json = Json.obj("b" -> "2".asJson, "c" -> "3".asJson, "d" -> "4".asJson)

      assert(statefulOpt.decodeJson(json).swap.exists(_.message === "Leftover keys: c, d"))
    }

    test("fail when there are leftover fields and an optional field is present") {
      val json = Json.obj("a" -> "1".asJson, "b" -> "2".asJson, "c" -> "3".asJson, "d" -> "4".asJson)

      assert(statefulOpt.decodeJson(json).swap.exists(_.message === "Leftover keys: c, d"))
    }

    test("fail normally when a field is missing and an optional field is present") {
      val json = Json.obj("a" -> "1".asJson)

      assert(statefulOpt.decodeJson(json).swap.exists(_.message === "Attempt to decode value on failed cursor"))
    }

    test("fail normally when a field is missing and an optional field is missing") {
      val json = Json.obj()

      assert(statefulOpt.decodeJson(json).swap.exists(_.message === "Attempt to decode value on failed cursor"))
    }
  }

  checkAll("Codec[WrappedOptionalField]", CodecTests[WrappedOptionalField].codec)

  property("decodeSet should match sequence decoders") {
    forAll { (xs: List[Int]) =>
      val sequence = Decoder[Seq[Int]].map(_.toSet).decodeJson(xs.asJson)
      assertEquals(Decoder.decodeSet[Int].decodeJson(xs.asJson), sequence)
    }
  }

  property("decodeList should match sequence decoders") {
    forAll { (xs: List[Int]) =>
      val sequence = Decoder[Seq[Int]].map(_.toList).decodeJson(xs.asJson)
      assertEquals(Decoder.decodeList[Int].decodeJson(xs.asJson), sequence)
    }
  }

  property("decodeVector should match sequence decoders") {
    forAll { (xs: List[Int]) =>
      val sequence = Decoder[Seq[Int]].map(_.toVector).decodeJson(xs.asJson)
      assertEquals(Decoder.decodeVector[Int].decodeJson(xs.asJson), sequence)
    }
  }

  property("decodeChain should match sequence decoders") {
    forAll { (xs: List[Int]) =>
      val sequence = Decoder[Seq[Int]].map(Chain.fromSeq(_)).decodeJson(xs.asJson)
      val chainDec = Decoder.decodeChain[Int].decodeJson(xs.asJson)
      assertEquals(sequence, chainDec)
    }
  }

  test("HCursor#history should be stack safe") {
    val size = 10000
    val json = List.fill(size)(1).asJson.mapArray(_ :+ true.asJson)
    val Left(DecodingFailure(_, history)) = Decoder[List[Int]].decodeJson(json)

    assertEquals(history.size, size + 1)
  }

  case class NotDecodable(a: Int)
  implicit val decodeNotDecodable: Decoder[NotDecodable] = Decoder.failedWithMessage("Some message")

  property("container decoder should pass through error message from item") {
    containerDecoders[NotDecodable].foreach { decoder =>
      val json = Json.arr(Json.obj("a" -> 1.asJson))
      val failure = DecodingFailure("Some message", List(DownArray))
      assertEquals(decoder.decodeJson(json), Left(failure))
    }
  }

  test("decodeAccumulating should accumulate errors after traversal") {
    import cats.syntax.functor._
    import cats.syntax.traverse._

    val decoder: Decoder[Unit] = List(
      Decoder[Int].ensure(_ > 0, "pos"),
      Decoder[Int].ensure(_ % 2 != 0, "odd")
    ).sequence.void

    val result = decoder.decodeAccumulating(Json.fromInt(-2).hcursor)

    assert(result.isInvalid)
    assertEquals(result.swap.toOption.map(_.size), Some(2))
  }
}
