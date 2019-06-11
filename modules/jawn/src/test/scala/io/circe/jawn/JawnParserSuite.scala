package io.circe.jawn

import org.scalatest.Matchers
import org.scalatest.funspec.AnyFunSpec

class JawnParserSuite extends AnyFunSpec with Matchers {
  describe("JawnParser") {
    it("should respect maxValueSize for numbers") {
      val parser = JawnParser(10)

      parser.parse("1000000000").isLeft shouldBe false
      parser.parse("[1000000000]").isLeft shouldBe false
      parser.parse("""{ "foo": 1000000000 }""").isLeft shouldBe false

      parser.parse("10000000000").isLeft shouldBe true
      parser.parse("[10000000000]").isLeft shouldBe true
      parser.parse("""{ "foo": 10000000000 }""").isLeft shouldBe true
    }

    it("should respect maxValueSize for strings") {
      val parser = JawnParser(10)

      parser.parse("\"1000000000\"").isLeft shouldBe false
      parser.parse("[\"1000000000\"]").isLeft shouldBe false
      parser.parse("""{ "foo": "1000000000" }""").isLeft shouldBe false

      parser.parse("\"10000000000\"").isLeft shouldBe true
      parser.parse("[\"10000000000\"]").isLeft shouldBe true
      parser.parse("""{ "foo": "10000000000" }""").isLeft shouldBe true
    }

    it("should respect maxValueSize for object keys") {
      val parser = JawnParser(10)

      parser.parse("""{ "1000000000": "foo" }""").isLeft shouldBe false

      parser.parse("""{ "10000000000": "foo" }""").isLeft shouldBe true
    }

    it("should respect allowedDuplicateKeys") {
      JawnParser(allowDuplicateKeys = true).parse("""{ "a": "b", "a": "c" }""").isLeft shouldBe false

      JawnParser(allowDuplicateKeys = false).parse("""{ "a": "b", "a": "c" }""").isLeft shouldBe true

    }
  }
}
