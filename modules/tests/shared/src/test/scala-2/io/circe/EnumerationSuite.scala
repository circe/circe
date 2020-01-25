package io.circe

import cats.kernel.Eq
import io.circe.parser.parse
import io.circe.syntax._
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import org.scalacheck.{ Arbitrary, Gen }

class EnumerationSuite extends CirceSuite {
  "Decoder[Enumeration]" should "parse Scala Enumerations" in {
    object WeekDay extends Enumeration {
      type WeekDay = Value
      val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
    }

    val decoder = Decoder.decodeEnumeration(WeekDay)
    val Right(friday) = parse("\"Fri\"")
    assert(decoder.apply(friday.hcursor) == Right(WeekDay.Fri))
  }

  "Decoder[Enumeration]" should "fail on unknown values in Scala Enumerations" in {
    object WeekDay extends Enumeration {
      type WeekDay = Value
      val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
    }

    val decoder = Decoder.decodeEnumeration(WeekDay)
    val Right(friday) = parse("\"Friday\"")

    assert(decoder.apply(friday.hcursor).isLeft)
  }

  "Encoder[Enumeration]" should "write Scala Enumerations" in {
    object WeekDay extends Enumeration {
      type WeekDay = Value
      val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
    }

    implicit val encoder = Encoder.encodeEnumeration(WeekDay)
    val json = WeekDay.Fri.asJson
    val decoder = Decoder.decodeEnumeration(WeekDay)
    assert(decoder.apply(json.hcursor) == Right(WeekDay.Fri))
  }
}

class EnumerationCodecSuite extends CirceSuite {
  object WeekDay extends Enumeration {
    type WeekDay = Value
    val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value

    implicit val arbitraryWeekDay: Arbitrary[WeekDay.WeekDay] = Arbitrary(
      Gen.oneOf(WeekDay.Mon, WeekDay.Tue, WeekDay.Wed, WeekDay.Thu, WeekDay.Fri, WeekDay.Sat, WeekDay.Sun)
    )
    implicit val eqWeekDay: Eq[WeekDay.WeekDay] = Eq.fromUniversalEquals
  }

  val decoder = Decoder.decodeEnumeration(WeekDay)
  val encoder = Encoder.encodeEnumeration(WeekDay)
  val codec = Codec.codecForEnumeration(WeekDay)

  checkAll("Codec[WeekDay.WeekDay]", CodecTests[WeekDay.WeekDay](decoder, encoder).unserializableCodec)
  checkAll("Codec[WeekDay.WeekDay] via Codec", CodecTests[WeekDay.WeekDay](codec, codec).unserializableCodec)
  checkAll(
    "Codec[WeekDay.WeekDay] via Decoder and Codec",
    CodecTests[WeekDay.WeekDay](decoder, codec).unserializableCodec
  )
  checkAll(
    "Codec[WeekDay.WeekDay] via Encoder and Codec",
    CodecTests[WeekDay.WeekDay](codec, encoder).unserializableCodec
  )
}
