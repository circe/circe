package io.circe

import cats.kernel.Eq
import io.circe.testing.CodecTests
import io.circe.tests.CirceMunitSuite
import java.time._
import org.scalacheck._
import org.scalacheck.Gen.Choose
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Prop.forAll
import scala.collection.JavaConverters._

case class JavaTimeCaseClass(foo: Duration, bar: Option[LocalTime], baz: List[ZoneId])

object JavaTimeCaseClass {
  implicit val decodeJavaTimeCaseClass: Decoder[JavaTimeCaseClass] =
    Decoder.forProduct3("foo", "bar", "baz")(JavaTimeCaseClass.apply)

  implicit val encodeJavaTimeCaseClass: Encoder.AsObject[JavaTimeCaseClass] =
    Encoder.forProduct3("foo", "bar", "baz") { (value: JavaTimeCaseClass) =>
      (value.foo, value.bar, value.baz)
    }
}

class JavaTimeCodecSuite extends CirceMunitSuite {
  private[this] val minInstant: Instant = Instant.EPOCH
  private[this] val maxInstant: Instant = Instant.parse("3000-01-01T00:00:00.00Z")

  // Backported from ScalaCheck
  // https://github.com/typelevel/scalacheck/blob/v1.16.0/core/shared/src/main/scala/org/scalacheck/time/JavaTimeChoose.scala#L225
  private[this] implicit final lazy val chooseZoneOffset: Choose[ZoneOffset] =
    new Choose[ZoneOffset] {
      override def choose(min: ZoneOffset, max: ZoneOffset): Gen[ZoneOffset] =
        min.compareTo(max) match {
          case 0                    => Gen.const(min)
          case result if result > 0 => Gen.fail
          case _                    =>
            // Looks flipped, but it is not.
            Gen.choose(max.getTotalSeconds, min.getTotalSeconds).map(value => ZoneOffset.ofTotalSeconds(value))
        }
    }

  implicit val arbitraryZoneOffset: Arbitrary[ZoneOffset] =
    Arbitrary(
      Gen.oneOf(
        Gen.oneOf(ZoneOffset.MAX, ZoneOffset.MIN, ZoneOffset.UTC),
        Gen.choose(ZoneOffset.MAX, ZoneOffset.MIN) // These look flipped, but they are not.
      )
    )

  implicit val arbitraryZoneId: Arbitrary[ZoneId] = Arbitrary(
    Gen.oneOf(ZoneId.getAvailableZoneIds.asScala.map(ZoneId.of).toSeq)
  )

  implicit val arbitraryInstant: Arbitrary[Instant] = Arbitrary(
    Gen.choose(minInstant.getEpochSecond, maxInstant.getEpochSecond).map(Instant.ofEpochSecond)
  )

  implicit val arbitraryPeriod: Arbitrary[Period] = Arbitrary(
    for {
      years <- arbitrary[Int]
      months <- arbitrary[Int]
      days <- arbitrary[Int]
    } yield Period.of(years, months, days)
  )

  implicit val arbitraryLocalDateTime: Arbitrary[LocalDateTime] = Arbitrary(
    for {
      instant <- arbitrary[Instant]
      offset <- arbitrary[ZoneOffset]
    } yield LocalDateTime.ofInstant(instant, offset)
  )

  implicit val arbitraryZonedDateTime: Arbitrary[ZonedDateTime] = Arbitrary(
    for {
      instant <- arbitrary[Instant]
      offset <- arbitrary[ZoneOffset]
    } yield ZonedDateTime.ofInstant(instant, offset)
  )

  implicit val arbitraryOffsetDateTime: Arbitrary[OffsetDateTime] = Arbitrary(
    for {
      instant <- arbitrary[Instant]
      offset <- arbitrary[ZoneOffset]
    } yield OffsetDateTime.ofInstant(instant, offset)
  )

  implicit val arbitraryLocalDate: Arbitrary[LocalDate] = Arbitrary(arbitrary[LocalDateTime].map(_.toLocalDate))

  implicit val arbitraryLocalTime: Arbitrary[LocalTime] = Arbitrary(arbitrary[LocalDateTime].map(_.toLocalTime))

  implicit val arbitraryMonthDay: Arbitrary[MonthDay] = Arbitrary(
    arbitrary[LocalDateTime].map(ldt => MonthDay.of(ldt.getMonth, ldt.getDayOfMonth))
  )

  implicit val arbitraryOffsetTime: Arbitrary[OffsetTime] = Arbitrary(arbitrary[OffsetDateTime].map(_.toOffsetTime))

  implicit val arbitraryYear: Arbitrary[Year] = Arbitrary(arbitrary[LocalDateTime].map(ldt => Year.of(ldt.getYear)))

  implicit val arbitraryYearMonth: Arbitrary[YearMonth] = Arbitrary(
    arbitrary[LocalDateTime].map(ldt => YearMonth.of(ldt.getYear, ldt.getMonth))
  )

  implicit val arbitraryDuration: Arbitrary[Duration] = Arbitrary(
    for {
      first <- arbitrary[Instant]
      second <- arbitrary[Instant]
    } yield Duration.between(first, second)
  )

  implicit val arbitraryJavaTimeCaseClass: Arbitrary[JavaTimeCaseClass] = Arbitrary(
    for {
      foo <- arbitrary[Duration]
      bar <- arbitrary[Option[LocalTime]]
      baz <- arbitrary[List[ZoneId]]
    } yield JavaTimeCaseClass(foo, bar, baz)
  )

  implicit val eqInstant: Eq[Instant] = Eq.fromUniversalEquals
  implicit val eqLocalDateTime: Eq[LocalDateTime] = Eq.fromUniversalEquals
  implicit val eqZonedDateTime: Eq[ZonedDateTime] = Eq.fromUniversalEquals
  implicit val eqOffsetDateTime: Eq[OffsetDateTime] = Eq.fromUniversalEquals
  implicit val eqLocalDate: Eq[LocalDate] = Eq.fromUniversalEquals
  implicit val eqLocalTime: Eq[LocalTime] = Eq.fromUniversalEquals
  implicit val eqMonthDay: Eq[MonthDay] = Eq.fromUniversalEquals
  implicit val eqOffsetTime: Eq[OffsetTime] = Eq.fromUniversalEquals
  implicit val eqPeriod: Eq[Period] = Eq.fromUniversalEquals
  implicit val eqYear: Eq[Year] = Eq.fromUniversalEquals
  implicit val eqYearMonth: Eq[YearMonth] = Eq.fromUniversalEquals
  implicit val eqDuration: Eq[Duration] = Eq.fromUniversalEquals
  implicit val eqZoneId: Eq[ZoneId] = Eq.fromUniversalEquals
  implicit val eqZoneOffset: Eq[ZoneOffset] = Eq.fromUniversalEquals
  implicit val eqJavaTimeCaseClass: Eq[JavaTimeCaseClass] = Eq.fromUniversalEquals

  checkAll("Codec[Instant]", CodecTests[Instant].codec)
  checkAll("Codec[LocalDateTime]", CodecTests[LocalDateTime].codec)
  checkAll("Codec[ZonedDateTime]", CodecTests[ZonedDateTime].codec)
  checkAll("Codec[OffsetDateTime]", CodecTests[OffsetDateTime].codec)
  checkAll("Codec[LocalDate]", CodecTests[LocalDate].codec)
  checkAll("Codec[LocalTime]", CodecTests[LocalTime].codec)
  checkAll("Codec[MonthDay]", CodecTests[MonthDay].codec)
  checkAll("Codec[OffsetTime]", CodecTests[OffsetTime].codec)
  checkAll("Codec[Period]", CodecTests[Period].codec)
  checkAll("Codec[Year]", CodecTests[Year].codec)
  checkAll("Codec[YearMonth]", CodecTests[YearMonth].codec)
  checkAll("Codec[Duration]", CodecTests[Duration].codec)
  checkAll("Codec[ZoneId]", CodecTests[ZoneId].codec)
  checkAll("Codec[ZoneOffset]", CodecTests[ZoneOffset].codec)
  checkAll("Codec[JavaTimeCaseClass]", CodecTests[JavaTimeCaseClass].codec)

  val invalidText: String = "abc"
  val invalidJson: Json = Json.fromString(invalidText)
  val parseExceptionMessage = s"Text '$invalidText'"

  property("Decoder[ZoneId] should fail for invalid ZoneId") {
    forAll((s: String) =>
      if (!ZoneId.getAvailableZoneIds.contains(s)) {
        val decodingResult = Decoder[ZoneId].decodeJson(Json.fromString(s))

        assert(decodingResult.isLeft)
        assert(decodingResult.swap.right.get.reason match {
          case DecodingFailure.Reason.CustomReason(_) => true
          case _                                      => false
        })
        assert(decodingResult.swap.exists(_.message.contains("invalid format")))
      }
    )
  }

  test("Decoder[Instant] should fail on invalid values") {
    val decodingResult = Decoder[Instant].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  test("Encoder[Instant] should serialize 00 seconds and drop zeroes in nanos to millis or micros") {
    def check(s: String): Unit =
      assert(Encoder[Instant].apply(Instant.parse(s)) == Json.fromString(s))

    check("2018-07-10T00:00:00Z")
    check("2018-07-10T00:00:00.100Z")
    check("2018-07-10T00:00:00.010Z")
    check("2018-07-10T00:00:00.001Z")
    check("2018-07-10T00:00:00.000100Z")
    check("2018-07-10T00:00:00.000010Z")
    check("2018-07-10T00:00:00.000001Z")
    check("2018-07-10T00:00:00.000000100Z")
    check("2018-07-10T00:00:00.000000010Z")
  }

  test("Decoder[LocalDateTime] should fail on invalid values") {
    val decodingResult = Decoder[LocalDateTime].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  test("Encoder[LocalDateTime] should serialize 00 seconds and drop all remaining zeroes in nanos") {
    def check(s: String): Unit =
      assert(Encoder[LocalDateTime].apply(LocalDateTime.parse(s)) == Json.fromString(s))

    check("2018-07-10T00:00:00")
    check("2018-07-10T00:00:00.1")
    check("2018-07-10T00:00:00.01")
    check("2018-07-10T00:00:00.001")
    check("2018-07-10T00:00:00.0001")
    check("2018-07-10T00:00:00.00001")
    check("2018-07-10T00:00:00.000001")
    check("2018-07-10T00:00:00.0000001")
    check("2018-07-10T00:00:00.00000001")
  }

  test("Decoder[ZonedDateTime] should fail on invalid values") {
    val decodingResult = Decoder[ZonedDateTime].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  test("Encoder[ZonedDateTime] should serialize 00 seconds and drop all remaining zeroes in nanos") {
    def check(s: String): Unit =
      assert(Encoder[ZonedDateTime].apply(ZonedDateTime.parse(s)) == Json.fromString(s))

    check("2018-07-10T00:00:00Z[UTC]")
    check("2018-07-10T00:00:00.1Z[UTC]")
    check("2018-07-10T00:00:00.01Z[UTC]")
    check("2018-07-10T00:00:00.001Z[UTC]")
    check("2018-07-10T00:00:00.0001Z[UTC]")
    check("2018-07-10T00:00:00.00001Z[UTC]")
    check("2018-07-10T00:00:00.000001Z[UTC]")
    check("2018-07-10T00:00:00.0000001Z[UTC]")
    check("2018-07-10T00:00:00.00000001Z[UTC]")
  }

  test("Decoder[OffsetDateTime] should fail on invalid values") {
    val decodingResult = Decoder[OffsetDateTime].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  test("Encoder[OffsetDateTime] should serialize 00 seconds and drop all remaining zeroes in nanos") {
    def check(s: String): Unit =
      assert(Encoder[OffsetDateTime].apply(OffsetDateTime.parse(s)) == Json.fromString(s))

    check("2018-07-10T00:00:00Z")
    check("2018-07-10T00:00:00.1Z")
    check("2018-07-10T00:00:00.01Z")
    check("2018-07-10T00:00:00.001Z")
    check("2018-07-10T00:00:00.0001Z")
    check("2018-07-10T00:00:00.00001Z")
    check("2018-07-10T00:00:00.000001Z")
    check("2018-07-10T00:00:00.0000001Z")
    check("2018-07-10T00:00:00.00000001Z")
  }

  test("Decoder[LocalDate] should fail on invalid values") {
    val decodingResult = Decoder[LocalDate].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  test("Decoder[LocalTime] should fail on invalid values") {
    val decodingResult = Decoder[LocalTime].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  test("Encoder[LocalTime] should serialize 00 seconds and drop all remaining zeroes in nanos") {
    def check(s: String): Unit =
      assert(Encoder[LocalTime].apply(LocalTime.parse(s)) == Json.fromString(s))

    check("00:00:00")
    check("00:00:00.1")
    check("00:00:00.01")
    check("00:00:00.001")
    check("00:00:00.0001")
    check("00:00:00.00001")
    check("00:00:00.000001")
    check("00:00:00.0000001")
    check("00:00:00.00000001")
  }

  test("Decoder[MonthDay] should fail on invalid values") {
    val decodingResult = Decoder[MonthDay].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  test("Decoder[OffsetTime] should fail on invalid values") {
    val decodingResult = Decoder[OffsetTime].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  test("Encoder[OffsetTime] should serialize 00 seconds and drop all remaining zeroes in nanos") {
    def check(s: String): Unit =
      assert(Encoder[OffsetTime].apply(OffsetTime.parse(s)) == Json.fromString(s))

    check("00:00:00Z")
    check("00:00:00.1Z")
    check("00:00:00.01Z")
    check("00:00:00.001Z")
    check("00:00:00.0001Z")
    check("00:00:00.00001Z")
    check("00:00:00.000001Z")
    check("00:00:00.0000001Z")
    check("00:00:00.00000001Z")
  }

  test("Decoder[Period] should fail on invalid values") {
    val decodingResult = Decoder[Period].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(s"Text '$invalidText' cannot be parsed to a Period")))
  }

  test("Decoder[Year] should fail on invalid values") {
    val decodingResult = Decoder[Year].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  test("Decoder[YearMonth] should fail on invalid values") {
    val decodingResult = Decoder[YearMonth].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  test("Decoder[Duration] should fail on invalid values") {
    val decodingResult = Decoder[Duration].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(s"Text '$invalidText' cannot be parsed to a Duration")))
  }

  test("Decoder[ZoneOffset] should fail on invalid values") {
    val decodingResult = Decoder[ZoneOffset].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.right.get.reason match {
      case DecodingFailure.Reason.CustomReason(_) => true
      case _                                      => false
    })
    assert(decodingResult.swap.exists(_.message.contains(invalidText)))
  }
}
