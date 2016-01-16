package io.circe.java8

import algebra.Eq
import io.circe.tests.{ CodecTests, CirceSuite }
import java.time.{ LocalDateTime, ZoneId }
import java.util.Date
import org.scalacheck.Arbitrary

class LocalDateTimeCodecSuite extends CirceSuite {
  implicit val localDateTimeArbitrary: Arbitrary[LocalDateTime] = Arbitrary(
    Arbitrary.arbitrary[Date].map(date =>
      LocalDateTime.ofInstant(date.toInstant, ZoneId.systemDefault)
    )
  )

  implicit val localDateTimeEq: Eq[LocalDateTime] = Eq.fromUniversalEquals

  checkAll("Codec[LocalDateTime]", CodecTests[LocalDateTime].codec)
}
