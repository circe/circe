package io.circe.refined.info

import io.circe.syntax._
import io.circe.tests.CirceSuite
import io.circe._
import io.circe.refined._
import io.circe.refined.info.classTag._
import org.scalatest.EitherValues

class RefinedClassTagInfoSuite extends CirceSuite with EitherValues {
  private val nameDecoder = Decoder[Person.Name]
  private val ageDecoder = Decoder[Person.Age]

  "A refined decoder with ClassTag type info" should "provide verbose error message" in {
    assert(
      nameDecoder.decodeJson("x".asJson).left.value.message ==
        "Failed to verify eu.timepit.refined.string$MatchesRegex[java.lang.String] refinement " +
          """for value x of raw type java.lang.String - Predicate failed: "x".matches("[A-z-]{2,}")."""
    )

    assert(
      ageDecoder.decodeJson((-2).asJson).left.value.message ==
        "Failed to verify eu.timepit.refined.boolean$Not[eu.timepit.refined.numeric$Less] refinement " +
          "for value -2 of raw type int - Predicate (-2 < 0) did not fail."
    )
  }
}
