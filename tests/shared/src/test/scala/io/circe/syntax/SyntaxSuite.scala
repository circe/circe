package io.circe

import io.circe.syntax._
import io.circe.tests.CirceSuite

class SyntaxSuite extends CirceSuite {
  test("EncodeOps.asJson") {
    check { (s: String) =>
      s.asJson === Json.fromString(s)
    }
  }
}
