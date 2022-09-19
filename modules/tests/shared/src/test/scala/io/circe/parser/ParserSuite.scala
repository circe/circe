package io.circe.parser

import io.circe.Json
import io.circe.testing.ParserTests
import io.circe.tests.CirceMunitSuite
import org.scalacheck.Prop._

class ParserSuite extends CirceMunitSuite {
  checkAll("Parser", ParserTests(`package`).fromString)

  property("parse and decode(Accumulating) should fail on invalid input") {
    forAll { (s: String) =>
      assert(parse(s"Not JSON $s").isLeft)
      assert(decode[Json](s"Not JSON $s").isLeft)
      assert(decodeAccumulating[Json](s"Not JSON $s").isInvalid)
    }
  }
}
