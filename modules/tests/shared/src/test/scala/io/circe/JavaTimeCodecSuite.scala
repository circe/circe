package io.circe

import cats.kernel.Eq
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import java.time._
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Arbitrary.arbitrary
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

class JavaTimeCodecSuite extends CirceSuite {
  private[this] val minInstant: Instant = Instant.EPOCH
  private[this] val maxInstant: Instant = Instant.parse("3000-01-01T00:00:00.00Z")

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
      zoneId <- arbitrary[ZoneId]
    } yield LocalDateTime.ofInstant(instant, zoneId)
  )

  implicit val arbitraryZonedDateTime: Arbitrary[ZonedDateTime] = Arbitrary(
    for {
      instant <- arbitrary[Instant]
      zoneId <- arbitrary[ZoneId].suchThat(_ != ZoneId.of("GMT0")) // #280 - avoid JDK-8138664
    } yield ZonedDateTime.ofInstant(instant, zoneId)
  )

  implicit val arbitraryOffsetDateTime: Arbitrary[OffsetDateTime] = Arbitrary(
    for {
      instant <- arbitrary[Instant]
      zoneId <- arbitrary[ZoneId]
    } yield OffsetDateTime.ofInstant(instant, zoneId)
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

  implicit val arbitraryZoneOffset: Arbitrary[ZoneOffset] =
    Arbitrary(Gen.choose(-18 * 60 * 60, 18 * 60 * 60).map(ZoneOffset.ofTotalSeconds))

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

  checkLaws("Codec[Instant]", CodecTests[Instant].codec)
  checkLaws("Codec[LocalDateTime]", CodecTests[LocalDateTime].codec)
  checkLaws("Codec[ZonedDateTime]", CodecTests[ZonedDateTime].codec)
  checkLaws("Codec[OffsetDateTime]", CodecTests[OffsetDateTime].codec)
  checkLaws("Codec[LocalDate]", CodecTests[LocalDate].codec)
  checkLaws("Codec[LocalTime]", CodecTests[LocalTime].codec)
  checkLaws("Codec[MonthDay]", CodecTests[MonthDay].codec)
  checkLaws("Codec[OffsetTime]", CodecTests[OffsetTime].codec)
  checkLaws("Codec[Period]", CodecTests[Period].codec)
  checkLaws("Codec[Year]", CodecTests[Year].codec)
  checkLaws("Codec[YearMonth]", CodecTests[YearMonth].codec)
  checkLaws("Codec[Duration]", CodecTests[Duration].codec)
  checkLaws("Codec[ZoneId]", CodecTests[ZoneId].codec)
  checkLaws("Codec[ZoneOffset]", CodecTests[ZoneOffset].codec)
  checkLaws("Codec[JavaTimeCaseClass]", CodecTests[JavaTimeCaseClass].codec)

  val invalidText: String = "abc"
  val invalidJson: Json = Json.fromString(invalidText)
  val parseExceptionMessage = s"Text '$invalidText'"

  "Decoder[ZoneId]" should "fail for invalid ZoneId" in {
    forAll(
      (s: String) =>
        whenever(!ZoneId.getAvailableZoneIds.contains(s)) {
          val decodingResult = Decoder[ZoneId].decodeJson(Json.fromString(s))

          assert(decodingResult.isLeft)
          // The middle part of the message depends on the type of zone.
          assert(decodingResult.swap.exists(_.message.contains("ZoneId (Invalid")))
          assert(decodingResult.swap.exists(_.message.contains(s", invalid format: $s)")))
        }
    )
  }

  "Decoder[Instant]" should "fail on invalid values" in {
    val decodingResult = Decoder[Instant].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  "Decoder[LocalDateTime]" should "fail on invalid values" in {
    val decodingResult = Decoder[LocalDateTime].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  "Decoder[ZonedDateTime]" should "fail on invalid values" in {
    val decodingResult = Decoder[ZonedDateTime].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  "Decoder[OffsetDateTime]" should "fail on invalid values" in {
    val decodingResult = Decoder[OffsetDateTime].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  "Decoder[LocalDate]" should "fail on invalid values" in {
    val decodingResult = Decoder[LocalDate].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  "Decoder[LocalTime]" should "fail on invalid values" in {
    val decodingResult = Decoder[LocalTime].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  "Decoder[MonthDay]" should "fail on invalid values" in {
    val decodingResult = Decoder[MonthDay].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  "Decoder[OffsetTime]" should "fail on invalid values" in {
    val decodingResult = Decoder[OffsetTime].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  "Decoder[Period]" should "fail on invalid values" in {
    val decodingResult = Decoder[Period].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(s"Text '$invalidText' cannot be parsed to a Period")))
  }

  "Decoder[Year]" should "fail on invalid values" in {
    val decodingResult = Decoder[Year].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  "Decoder[YearMonth]" should "fail on invalid values" in {
    val decodingResult = Decoder[YearMonth].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(parseExceptionMessage)))
  }

  "Decoder[Duration]" should "fail on invalid values" in {
    val decodingResult = Decoder[Duration].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains(s"Text '$invalidText' cannot be parsed to a Duration")))
  }

  "Decoder[ZoneOffset]" should "fail on invalid values" in {
    val decodingResult = Decoder[ZoneOffset].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.swap.exists(_.message.contains("ZoneOffset (Invalid ID for ZoneOffset, ")))
  }
}
