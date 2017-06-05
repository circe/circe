package io.circe

import io.circe.numbers.JsonNumber
import io.circe.tests.CirceSuite

/**
 * Tests that fail because of limitations on Scala.js.
 */
trait FloatJsonTests { this: CirceSuite =>
  "fromFloatOrString" should "return JNumber for valid Floats" in {
    assert(Json.fromFloatOrString(1.1f) === Json.JNumber(JsonNumber.parseJsonNumberUnsafe("1.1")))
    assert(Json.fromFloatOrString(-1.2f) === Json.JNumber(JsonNumber.parseJsonNumberUnsafe("-1.2")))
  }
}
