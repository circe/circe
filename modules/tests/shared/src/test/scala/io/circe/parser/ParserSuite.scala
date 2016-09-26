package io.circe.parser

import io.circe.testing.ParserTests
import io.circe.tests.CirceSuite

class ParserSuite extends CirceSuite {
  checkLaws("Parser", ParserTests(`package`).parser)

  "parse" should "fail on invalid input" in forAll { (s: String) =>
    assert(parse(s"Not JSON $s").isLeft)
  }
}
