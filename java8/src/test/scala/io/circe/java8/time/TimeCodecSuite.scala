package io.circe.java8.time

import algebra.Eq
import io.circe.tests.{ CodecTests, CirceSuite }
import java.time.{ Instant, LocalDate, LocalDateTime, OffsetDateTime, ZoneId, ZonedDateTime }
import java.util.Date
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Arbitrary.arbitrary
import scala.collection.JavaConverters._

class LocalDateTimeCodecSuite extends CirceSuite {
  implicit val arbitraryZoneId: Arbitrary[ZoneId] = Arbitrary(
    Gen.oneOf(ZoneId.getAvailableZoneIds.asScala.map(ZoneId.of).toSeq)
  )

  implicit val arbitraryInstant: Arbitrary[Instant] = Arbitrary(arbitrary[Date].map(_.toInstant))

  implicit val arbitraryLocalDateTime: Arbitrary[LocalDateTime] = Arbitrary(
    for {
      instant <- arbitrary[Instant]
      zoneId  <- arbitrary[ZoneId]
    } yield LocalDateTime.ofInstant(instant, zoneId)
  )

  implicit val arbitraryZonedDateTime: Arbitrary[ZonedDateTime] = Arbitrary(
    for {
      instant <- arbitrary[Instant]
      zoneId  <- arbitrary[ZoneId]
    } yield ZonedDateTime.ofInstant(instant, zoneId)
  )

  implicit val arbitraryOffsetDateTime: Arbitrary[OffsetDateTime] = Arbitrary(
    for {
      instant <- arbitrary[Instant]
      zoneId  <- arbitrary[ZoneId]
    } yield OffsetDateTime.ofInstant(instant, zoneId)
  )

  implicit val arbitraryLocalDate: Arbitrary[LocalDate] = Arbitrary(arbitrary[LocalDateTime].map(_.toLocalDate))


  implicit val eqInstant: Eq[Instant] = Eq.fromUniversalEquals
  implicit val eqLocalDateTime: Eq[LocalDateTime] = Eq.fromUniversalEquals
  implicit val eqZonedDateTime: Eq[ZonedDateTime] = Eq.fromUniversalEquals
  implicit val eqOffsetDateTime: Eq[OffsetDateTime] = Eq.fromUniversalEquals
  implicit val eqLocalDate: Eq[LocalDate] = Eq.fromUniversalEquals

  checkAll("Codec[Instant]", CodecTests[Instant].codec)
  checkAll("Codec[LocalDateTime]", CodecTests[LocalDateTime].codec)
  checkAll("Codec[ZonedDateTime]", CodecTests[ZonedDateTime].codec)
  checkAll("Codec[OffsetDateTime]", CodecTests[OffsetDateTime].codec)
  checkAll("Codec[LocalDate]", CodecTests[LocalDate].codec)
}
