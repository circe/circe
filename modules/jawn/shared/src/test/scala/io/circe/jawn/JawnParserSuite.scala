package io.circe.jawn

import munit.FunSuite

class JawnParserSuite extends FunSuite {
  test("JawnParser should respect maxValueSize for numbers") {
    val parser = JawnParser(10)

    assert(parser.parse("1000000000").isRight)
    assert(parser.parse("[1000000000]").isRight)
    assert(parser.parse("""{ "foo": 1000000000 }""").isRight)

    assert(parser.parse("10000000000").isLeft)
    assert(parser.parse("[10000000000]").isLeft)
    assert(parser.parse("""{ "foo": 10000000000 }""").isLeft)
  }
  test("JawnParser should respect maxValueSize for strings") {
    val parser = JawnParser(10)

    assert(parser.parse("\"1000000000\"").isRight)
    assert(parser.parse("[\"1000000000\"]").isRight)
    assert(parser.parse("""{ "foo": "1000000000" }""").isRight)

    assert(parser.parse("\"10000000000\"").isLeft)
    assert(parser.parse("[\"10000000000\"]").isLeft)
    assert(parser.parse("""{ "foo": "10000000000" }""").isLeft)
  }

  test("JawnParser should respect maxValueSize for object keys") {
    val parser = JawnParser(10)

    assert(parser.parse("""{ "1000000000": "foo" }""").isRight)
    assert(parser.parse("""{ "10000000000": "foo" }""").isLeft)
  }

  test("JawnParser should respect allowedDuplicateKeys") {
    assert(JawnParser(allowDuplicateKeys = true).parse("""{ "a": "b", "a": "c" }""").isRight)
    assert(JawnParser(allowDuplicateKeys = false).parse("""{ "a": "b", "a": "c" }""").isLeft)
  }
}
