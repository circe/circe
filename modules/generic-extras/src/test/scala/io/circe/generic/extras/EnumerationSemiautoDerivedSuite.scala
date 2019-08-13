package io.circe.generic.extras

import io.circe.{ Codec, Decoder, Encoder }
import io.circe.generic.extras.semiauto._
import io.circe.literal._
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import io.circe.tests.examples._
import shapeless.test.illTyped

class EnumerationSemiautoDerivedSuite extends CirceSuite {
  implicit val decodeCardinalDirection: Decoder[CardinalDirection] = deriveEnumerationDecoder
  implicit val encodeCardinalDirection: Encoder[CardinalDirection] = deriveEnumerationEncoder
  val codecForCardinalDirection: Codec[CardinalDirection] = deriveEnumerationCodec

  checkLaws("Codec[CardinalDirection]", CodecTests[CardinalDirection].codec)
  checkLaws(
    "Codec[CardinalDirection] via Codec",
    CodecTests[CardinalDirection](codecForCardinalDirection, codecForCardinalDirection).codec
  )
  checkLaws(
    "Codec[CardinalDirection] via Decoder and Codec",
    CodecTests[CardinalDirection](implicitly, codecForCardinalDirection).codec
  )
  checkLaws(
    "Codec[CardinalDirection] via Encoder and Codec",
    CodecTests[CardinalDirection](codecForCardinalDirection, implicitly).codec
  )

  "deriveEnumerationDecoder" should "not compile on an ADT with case classes" in {
    implicit val config: Configuration = Configuration.default
    illTyped("deriveEnumerationDecoder[ExtendedCardinalDirection]")
  }

  it should "respect Configuration" in {
    implicit val config: Configuration = Configuration.default.withSnakeCaseConstructorNames
    val decodeMary = deriveEnumerationDecoder[Mary]
    val expected = json""""little_lamb""""
    assert(decodeMary.decodeJson(expected) === Right(LittleLamb))
  }

  "deriveEnumerationEncoder" should "not compile on an ADT with case classes" in {
    implicit val config: Configuration = Configuration.default
    illTyped("deriveEnumerationEncoder[ExtendedCardinalDirection]")
  }

  it should "respect Configuration" in {
    implicit val config: Configuration = Configuration.default.withSnakeCaseConstructorNames
    val encodeMary = deriveEnumerationEncoder[Mary]
    val expected = json""""little_lamb""""
    assert(encodeMary(LittleLamb) === expected)
  }
}
