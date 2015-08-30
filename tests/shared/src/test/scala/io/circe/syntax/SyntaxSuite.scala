package io.circe

import io.circe.syntax._
import io.circe.tests.CirceSuite
import org.scalacheck.Prop.forAll

class SyntaxSuite extends CirceSuite {
  test("EncodeOps.asJson") {
    check {
      forAll { (s: String) =>
        s.asJson === Json.string(s)
      }
    }
  }
}
