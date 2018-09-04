package io.circe

import cats.kernel.Eq
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import java.time.{
  Duration,
  Instant,
  LocalDate,
  LocalDateTime,
  LocalTime,
  OffsetDateTime,
  OffsetTime,
  Period,
  YearMonth,
  ZonedDateTime,
  ZoneId
}
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Arbitrary.arbitrary
import scala.collection.JavaConverters._

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

  implicit val arbitraryOffsetTime: Arbitrary[OffsetTime] = Arbitrary(arbitrary[OffsetDateTime].map(_.toOffsetTime))

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
  implicit val eqOffsetTime: Eq[OffsetTime] = Eq.fromUniversalEquals
  implicit val eqPeriod: Eq[Period] = Eq.fromUniversalEquals
  implicit val eqYearMonth: Eq[YearMonth] = Eq.fromUniversalEquals
  implicit val eqDuration: Eq[Duration] = Eq.fromUniversalEquals
  implicit val eqZoneId: Eq[ZoneId] = Eq.fromUniversalEquals

  checkLaws("Codec[Instant]", CodecTests[Instant].codec)
  checkLaws("Codec[LocalDateTime]", CodecTests[LocalDateTime].unserializableCodec)
  checkLaws("Codec[ZonedDateTime]", CodecTests[ZonedDateTime].unserializableCodec)
  checkLaws("Codec[OffsetDateTime]", CodecTests[OffsetDateTime].unserializableCodec)
  checkLaws("Codec[LocalDate]", CodecTests[LocalDate].unserializableCodec)
  checkLaws("Codec[LocalTime]", CodecTests[LocalTime].unserializableCodec)
  checkLaws("Codec[OffsetTime]", CodecTests[OffsetTime].unserializableCodec)
  checkLaws("Codec[Period]", CodecTests[Period].codec)
  checkLaws("Codec[YearMonth]", CodecTests[YearMonth].unserializableCodec)
  checkLaws("Codec[Duration]", CodecTests[Duration].codec)
  checkLaws("Codec[ZoneId]", CodecTests[ZoneId].codec)

  val invalidText: String = "abc"
  val invalidJson: Json = Json.fromString(invalidText)
  val parseExceptionMessage = s"Text '$invalidText'"

  "Decoder[ZoneId]" should "fail for invalid ZoneId" in {
    forAll((s: String) =>
      whenever(!ZoneId.getAvailableZoneIds.contains(s)) {
        val decodingResult = Decoder[ZoneId].decodeJson(Json.fromString(s))

        assert(decodingResult.isLeft)
        // The middle part of the message depends on the type of zone.
        assert(decodingResult.left.get.message.contains("ZoneId (Invalid"))
        assert(decodingResult.left.get.message.contains(s", invalid format: $s)"))
      }
    )
  }

  "Decoder[Instant]" should "fail on invalid values" in {
    val decodingResult = Decoder[Instant].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.left.get.message.contains(parseExceptionMessage))
  }

  "Decoder[LocalDateTime]" should "fail on invalid values" in {
    val decodingResult = Decoder[LocalDateTime].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.left.get.message.contains(parseExceptionMessage))
  }

  "Decoder[ZonedDateTime]" should "fail on invalid values" in {
    val decodingResult = Decoder[ZonedDateTime].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.left.get.message.contains(parseExceptionMessage))
  }

  "Decoder[OffsetDateTime]" should "fail on invalid values" in {
    val decodingResult = Decoder[OffsetDateTime].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.left.get.message.contains(parseExceptionMessage))
  }

  "Decoder[LocalDate]" should "fail on invalid values" in {
    val decodingResult = Decoder[LocalDate].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.left.get.message.contains(parseExceptionMessage))
  }

  "Decoder[LocalTime]" should "fail on invalid values" in {
    val decodingResult = Decoder[LocalTime].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.left.get.message.contains(parseExceptionMessage))
  }

  "Decoder[OffsetTime]" should "fail on invalid values" in {
    val decodingResult = Decoder[OffsetTime].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.left.get.message.contains(parseExceptionMessage))
  }

  "Decoder[Period]" should "fail on invalid values" in {
    val decodingResult = Decoder[Period].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.left.get.message.contains(s"Text '$invalidText' cannot be parsed to a Period"))
  }

  "Decoder[YearMonth]" should "fail on invalid values" in {
    val decodingResult = Decoder[YearMonth].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.left.get.message.contains(parseExceptionMessage))
  }

  "Decoder[Duration]" should "fail on invalid values" in {
    val decodingResult = Decoder[Duration].apply(invalidJson.hcursor)

    assert(decodingResult.isLeft)
    assert(decodingResult.left.get.message.contains(s"Text '$invalidText' cannot be parsed to a Duration"))
  }
}
