package io.circe.parser

import io.circe.Json
import io.circe.testing.ParserTests
import io.circe.tests.CirceSuite

class ParserSuite extends CirceSuite {
  checkLaws("Parser", ParserTests(`package`).fromString)

  "parse and decode(Accumulating)" should "fail on invalid input" in forAll { (s: String) =>
    assert(parse(s"Not JSON $s").isLeft)
    assert(decode[Json](s"Not JSON $s").isLeft)
    assert(decodeAccumulating[Json](s"Not JSON $s").isInvalid)
  }
}
