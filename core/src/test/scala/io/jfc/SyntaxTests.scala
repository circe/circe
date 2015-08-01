package io.jfc

import cats.data.{ NonEmptyList, Validated, Xor }
import io.jfc.syntax._
import io.jfc.test.{ CodecTests, JfcSuite }
import org.scalacheck.Prop.forAll

class SyntaxTests extends JfcSuite {
  test("EncodeOps.toJson") {
    check {
      forAll { (s: String) =>
        s.toJson === Json.string(s)
      }
    }
  }
}
