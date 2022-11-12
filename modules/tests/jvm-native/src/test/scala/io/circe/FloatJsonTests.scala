package io.circe

import cats.syntax.eq._
import io.circe.tests.CirceMunitSuite

/**
 * Tests that fail because of limitations on Scala.js.
 */
trait FloatJsonTests { this: CirceMunitSuite =>
  test("fromFloatOrString should return a Json number for valid Floats") {
    assertEquals(Json.fromFloatOrString(1.1f), Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe("1.1")))
    assertEquals(Json.fromFloatOrString(-1.2f), Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe("-1.2")))
  }
}
