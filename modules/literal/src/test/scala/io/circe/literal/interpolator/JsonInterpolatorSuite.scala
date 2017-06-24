package io.circe.literal.interpolator

import io.circe.{ Encoder, Json }
import io.circe.literal.JsonStringContext
import io.circe.parser.parse
import io.circe.testing.instances.arbitraryJson
import org.scalatest.{ FunSpec, Matchers }
import org.scalatest.prop.GeneratorDrivenPropertyChecks

class JsonInterpolatorSuite extends FunSpec with Matchers with GeneratorDrivenPropertyChecks {
  describe("The json string interpolater") {
    it("should fail to compile with invalid JSON") {
      "json\"\"\"1a2b3c\"\"\"" shouldNot compile
    }

    it("should work with no interpolated variables") {
      val interpolated = json"""
        {
          "a": [1, 2, 3],
          "b": { "foo": false },
          "c": null,
          "d": "bar",
          "e": 0.0001
        }
      """

      val parsed = parse(
        """
          {
            "a": [1, 2, 3],
            "b": { "foo": false },
            "c": null,
            "d": "bar",
            "e": 0.0001
          }
        """
      )

      parsed shouldBe Right(interpolated)
    }

    describe("with interpolation in JSON value positions") {
      it("should work with interpolated variables") {
        val i = 13
        val m = Map("bar" -> List(1, 2, 3), "baz" -> Nil)
        val interpolated = json"""{ "i": $i, "ms": [$m, $m], "other": [1.0, "abc"] }"""
        val parsed = parse(
          """
            {
              "i": 13,
              "ms": [
                { "bar": [1, 2, 3], "baz": [] },
                { "bar": [1, 2, 3], "baz": [] }
              ],
              "other": [1.0, "abc"]
            }
          """
        )

        parsed shouldBe Right(interpolated)
      }

      it("should work with interpolated literals") {
        val interpolated = json"""{ "k": ${ 1 } }"""
        val parsed = parse(s"""{ "k": 1 }""")

        parsed shouldBe Right(interpolated)
      }

      it("should fail with unencodeable interpolated variables") {
        trait Foo
        val foo = new Foo {}

        "json\"$foo\"" shouldNot compile
      }
    }

    describe("with interpolation in JSON key positions") {
      it("should work with interpolated string variables") {
        forAll { (key: String, value: Json) =>
          val interpolated = json"{ $key: $value }"
          val escapedKey = Encoder[String].apply(key).noSpaces
          val parsed = parse(s"{ $escapedKey: ${ value.noSpaces } }")

          parsed shouldBe Right(interpolated)
        }
      }

      it("should work with interpolated non-string variables") {
        forAll { (key: Int, value: Json) =>
          val interpolated = json"{ $key: $value }"
          val parsed = parse(s"""{ "${ key.toString }": ${ value.noSpaces } }""")

          parsed shouldBe Right(interpolated)
        }
      }

      it("should work with interpolated literals") {
        val interpolated = json"""{ ${ 1 }: "v" }"""
        val parsed = parse(s"""{ "1": "v" }""")

        parsed shouldBe Right(interpolated)
      }

      it("should fail with unencodeable interpolated variables") {
        trait Foo
        val foo = new Foo {}

        "json\"{ $foo: 1 }\"" shouldNot compile
      }
    }
  }
}
