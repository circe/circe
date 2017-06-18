package io.circe.generic.extras

import io.circe.{ Decoder, Encoder }
import io.circe.generic.extras.semiauto._
import io.circe.literal._
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import io.circe.tests.examples._
import shapeless.test.illTyped

class EnumerationSemiautoDerivedSuite extends CirceSuite {
  implicit val decodeCardinalDirection: Decoder[CardinalDirection] = deriveEnumerationDecoder
  implicit val encodeCardinalDirection: Encoder[CardinalDirection] = deriveEnumerationEncoder

  checkLaws("Codec[CardinalDirection]", CodecTests[CardinalDirection].codec)

  "deriveEnumerationDecoder" should "not compile on an ADT with case classes" in {
     illTyped("deriveEnumerationDecoder[ExtendedCardinalDirection]")
   }
 
   "deriveEnumerationEncoder" should "not compile on an ADT with case classes" in {
     illTyped("deriveEnumerationEncoder[ExtendedCardinalDirection]")
   }
}
