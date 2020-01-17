package io.circe.literal.interpolator

import io.circe.{ Encoder, Json }
import io.circe.literal.JsonStringContext
import io.circe.parser.parse
import io.circe.testing.instances.arbitraryJson
import org.scalatest.funspec.AnyFunSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class JsonInterpolatorSuite extends AnyFunSpec with ScalaCheckDrivenPropertyChecks {
  describe("The json string interpolater") {
    it("should fail to compile with invalid JSON") {
      assertDoesNotCompile("json\"\"\"1a2b3c\"\"\"")
    }

    describe("should work with top-level") {
      it("null") {
        assert(json"null" === Json.Null)
      }

      it("booleans") {
        forAll { (value: Boolean) =>
          assert(json"$value" === Json.fromBoolean(value))
        }
      }

      it("strings") {
        forAll { (value: String) =>
          assert(json"$value" === Json.fromString(value))
        }
      }

      it("numbers") {
        forAll { (value: BigDecimal) =>
          assert(json"$value" === Json.fromBigDecimal(value))
        }
      }
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

      assert(parsed === Right(interpolated))
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

        assert(parsed === Right(interpolated))
      }

      it("should work with interpolated literals") {
        val interpolated = json"""{ "k": ${1} }"""
        val parsed = parse(s"""{ "k": 1 }""")

        assert(parsed === Right(interpolated))
      }

      it("should fail with unencodeable interpolated variables") {
        trait Foo
        val foo = new Foo {}

        assertDoesNotCompile("json\"$foo\"")
      }
    }

    describe("with interpolation in JSON key positions") {
      it("should work with interpolated string variables") {
        forAll { (key: String, value: Json) =>
          val interpolated = json"{ $key: $value }"
          val escapedKey = Encoder[String].apply(key).noSpaces
          val parsed = parse(s"{ $escapedKey: ${value.noSpaces} }")

          assert(parsed === Right(interpolated))
        }
      }

      it("should work with interpolated non-string variables") {
        forAll { (key: Int, value: Json) =>
          val interpolated = json"{ $key: $value }"
          val parsed = parse(s"""{ "${key.toString}": ${value.noSpaces} }""")

          assert(parsed === Right(interpolated))
        }
      }

      it("should work with interpolated literals") {
        val interpolated = json"""{ ${1}: "v" }"""
        val parsed = parse(s"""{ "1": "v" }""")

        assert(parsed === Right(interpolated))
      }

      it("should fail with unencodeable interpolated variables") {
        trait Foo
        val foo = new Foo {}

        assertDoesNotCompile("json\"{ $foo: 1 }\"")
      }
    }
  }
}
