package io.circe.generic.extras

import io.circe.generic.extras.semiauto._
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import io.circe.tests.examples._
import io.circe.Codec
import shapeless.test.illTyped

class EnumerationSemiautoDerivedCodecSuite extends CirceSuite {
  implicit val codecCardinalDirection: Codec[CardinalDirection] = deriveEnumerationCodec

  checkLaws("Codec[CardinalDirection]", CodecTests[CardinalDirection].codec)

  "deriveEnumerationDecoder" should "not compile on an ADT with case classes" in {
     illTyped("deriveEnumerationDecoder[ExtendedCardinalDirection]")
   }

   "deriveEnumerationEncoder" should "not compile on an ADT with case classes" in {
     illTyped("deriveEnumerationEncoder[ExtendedCardinalDirection]")
   }
}
