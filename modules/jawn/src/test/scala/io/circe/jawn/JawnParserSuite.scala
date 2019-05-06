package io.circe.jawn

import org.scalacheck.Properties
import org.typelevel.claimant.Claim

class JawnParserSuite extends Properties("JawnParser") {
  property("should respect maxValueSize for numbers") = {
    val parser = JawnParser(10)

    Claim(parser.parse("1000000000").isLeft == false) &&
    Claim(parser.parse("[1000000000]").isLeft == false) &&
    Claim(parser.parse("""{ "foo": 1000000000 }""").isLeft == false) &&
    Claim(parser.parse("10000000000").isLeft == true) &&
    Claim(parser.parse("[10000000000]").isLeft == true) &&
    Claim(parser.parse("""{ "foo": 10000000000 }""").isLeft == true)
  }

  property("should respect maxValueSize for strings") = {
    val parser = JawnParser(10)

    Claim(parser.parse("\"1000000000\"").isLeft == false) &&
    Claim(parser.parse("[\"1000000000\"]").isLeft == false) &&
    Claim(parser.parse("""{ "foo": "1000000000" }""").isLeft == false) &&
    Claim(parser.parse("\"10000000000\"").isLeft == true) &&
    Claim(parser.parse("[\"10000000000\"]").isLeft == true) &&
    Claim(parser.parse("""{ "foo": "10000000000" }""").isLeft == true)
  }

  property("should respect maxValueSize for object keys") = {
    val parser = JawnParser(10)

    Claim(parser.parse("""{ "1000000000": "foo" }""").isLeft == false) &&
    Claim(parser.parse("""{ "10000000000": "foo" }""").isLeft == true)
  }
}
