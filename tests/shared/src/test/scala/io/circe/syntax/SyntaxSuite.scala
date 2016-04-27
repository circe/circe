package io.circe.syntax

import io.circe.Json
import io.circe.tests.CirceSuite

class SyntaxSuite extends CirceSuite {
  "asJson" should "be available and work appropriately" in forAll { (s: String) =>
    assert(s.asJson === Json.fromString(s))
  }
}
