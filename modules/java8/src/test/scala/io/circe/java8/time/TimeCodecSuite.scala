package io.circe.java8.time

import cats.Eq
import io.circe.{ Decoder, Json }
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import java.time.{ Duration, Instant, LocalDate, LocalDateTime, LocalTime, OffsetDateTime, Period, YearMonth, ZonedDateTime, ZoneId }
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Arbitrary.arbitrary
import scala.collection.JavaConverters._

class TimeCodecSuite extends CirceSuite {
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
      zoneId  <- arbitrary[ZoneId]
    } yield LocalDateTime.ofInstant(instant, zoneId)
  )

  implicit val arbitraryZonedDateTime: Arbitrary[ZonedDateTime] = Arbitrary(
    for {
      instant <- arbitrary[Instant]
      zoneId  <- arbitrary[ZoneId].suchThat(_ != ZoneId.of("GMT0")) // #280 - avoid JDK-8138664
    } yield ZonedDateTime.ofInstant(instant, zoneId)
  )

  implicit val arbitraryOffsetDateTime: Arbitrary[OffsetDateTime] = Arbitrary(
    for {
      instant <- arbitrary[Instant]
      zoneId  <- arbitrary[ZoneId]
    } yield OffsetDateTime.ofInstant(instant, zoneId)
  )

  implicit val arbitraryLocalDate: Arbitrary[LocalDate] = Arbitrary(arbitrary[LocalDateTime].map(_.toLocalDate))

  implicit val arbitraryLocalTime: Arbitrary[LocalTime] = Arbitrary(arbitrary[LocalDateTime].map(_.toLocalTime))

  implicit val arbitraryYearMonth: Arbitrary[YearMonth] = Arbitrary(arbitrary[LocalDateTime].map(
    ldt => YearMonth.of(ldt.getYear, ldt.getMonth)))

  implicit val arbitraryDuration: Arbitrary[Duration] = Arbitrary(
    for {
      first <- arbitrary[Instant]
      second <- arbitrary[Instant]
    } yield Duration.between(first, second)
  )

  implicit val eqInstant: Eq[Instant] = Eq.fromUniversalEquals
  implicit val eqLocalDateTime: Eq[LocalDateTime] = Eq.fromUniversalEquals
  implicit val eqZonedDateTime: Eq[ZonedDateTime] = Eq.fromUniversalEquals
  implicit val eqOffsetDateTime: Eq[OffsetDateTime] = Eq.fromUniversalEquals
  implicit val eqLocalDate: Eq[LocalDate] = Eq.fromUniversalEquals
  implicit val eqLocalTime: Eq[LocalTime] = Eq.fromUniversalEquals
  implicit val eqPeriod: Eq[Period] = Eq.fromUniversalEquals
  implicit val eqYearMonth: Eq[YearMonth] = Eq.fromUniversalEquals
  implicit val eqDuration: Eq[Duration] = Eq.fromUniversalEquals

  checkLaws("Codec[Instant]", CodecTests[Instant].codec)
  checkLaws("Codec[LocalDateTime]", CodecTests[LocalDateTime].codec)
  checkLaws("Codec[ZonedDateTime]", CodecTests[ZonedDateTime].codec)
  checkLaws("Codec[OffsetDateTime]", CodecTests[OffsetDateTime].codec)
  checkLaws("Codec[LocalDate]", CodecTests[LocalDate].codec)
  checkLaws("Codec[LocalTime]", CodecTests[LocalTime].codec)
  checkLaws("Codec[Period]", CodecTests[Period].codec)
  checkLaws("Codec[YearMonth]", CodecTests[YearMonth].codec)
  checkLaws("Codec[Duration]", CodecTests[Duration].codec)

  val invalidJson: Json = Json.fromString("abc")

  "Decoder[Instant]" should "fail on invalid values" in {
    assert(Decoder[Instant].apply(invalidJson.hcursor).isLeft)
  }

  "Decoder[LocalDateTime]" should "fail on invalid values" in {
    assert(Decoder[LocalDateTime].apply(invalidJson.hcursor).isLeft)
  }

  "Decoder[ZonedDateTime]" should "fail on invalid values" in {
    assert(Decoder[ZonedDateTime].apply(invalidJson.hcursor).isLeft)
  }

  "Decoder[OffsetDateTime]" should "fail on invalid values" in {
    assert(Decoder[OffsetDateTime].apply(invalidJson.hcursor).isLeft)
  }

  "Decoder[LocalDate]" should "fail on invalid values" in {
    assert(Decoder[LocalDate].apply(invalidJson.hcursor).isLeft)
  }

  "Decoder[LocalTime]" should "fail on invalid values" in {
    assert(Decoder[LocalTime].apply(invalidJson.hcursor).isLeft)
  }

  "Decoder[Period]" should "fail on invalid values" in {
    assert(Decoder[Period].apply(invalidJson.hcursor).isLeft)
  }

  "Decoder[YearMonth]" should "fail on invalid values" in {
    assert(Decoder[YearMonth].apply(invalidJson.hcursor).isLeft)
  }

  "Decoder[Duration]" should "fail on invalid values" in {
    assert(Decoder[Duration].apply(invalidJson.hcursor).isLeft)
  }
}
