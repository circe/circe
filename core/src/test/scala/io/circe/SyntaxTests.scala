package io.circe

import cats.data.{ NonEmptyList, Validated, Xor }
import io.circe.syntax._
import io.circe.test.{ CodecTests, JfcSuite }
import org.scalacheck.Prop.forAll

class SyntaxTests extends JfcSuite {
  test("EncodeOps.asJson") {
    check {
      forAll { (s: String) =>
        s.asJson === Json.string(s)
      }
    }
  }
}
