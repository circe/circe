package io.circe.parse

import io.circe.tests.{ CirceSuite, ParserTests }

class ParserSuite extends CirceSuite {
  checkAll("Parser", ParserTests(`package`).parser)

  test("Parsing should fail on invalid input") {
    check { (s: String) =>
      parse(s"Not JSON $s").isLeft
    }
  }
}
