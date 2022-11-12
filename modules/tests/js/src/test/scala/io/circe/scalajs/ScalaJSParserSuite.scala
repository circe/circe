package io.circe.scalajs

import io.circe.Json
import io.circe.testing.ParserTests
import io.circe.tests.CirceMunitSuite
import org.scalacheck.Prop.forAll

class ScalaJSParserSuite extends CirceMunitSuite {
  checkAll("Parser", ParserTests(ScalaJSParser).fromString)

  property("parse and decode(Accumulating) should fail on invalid input") {
    forAll { (s: String) =>
      import ScalaJSParser._

      assert(parse(s"Not JSON $s").isLeft)
      assert(decode[Json](s"Not JSON $s").isLeft)
      assert(decodeAccumulating[Json](s"Not JSON $s").isInvalid)
    }
  }
}
