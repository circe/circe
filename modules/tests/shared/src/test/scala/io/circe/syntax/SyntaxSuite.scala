package io.circe.syntax

import io.circe.{ Encoder, Json }
import io.circe.tests.CirceSuite

class SyntaxSuite extends CirceSuite {
  "asJson" should "be available and work appropriately" in forAll { (s: String) =>
    assert(s.asJson === Json.fromString(s))
  }

  "asJsonObject" should "be available and work appropriately" in forAll { (m: Map[String, Int]) =>
    assert(m.asJsonObject === Encoder[Map[String, Int]].apply(m).asObject.get)
  }
}
