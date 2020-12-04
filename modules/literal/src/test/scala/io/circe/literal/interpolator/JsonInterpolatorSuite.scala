package io.circe.literal.interpolator

import io.circe.{ Encoder, Json }
import io.circe.literal.JsonStringContext
import io.circe.parser.parse
import io.circe.testing.instances.arbitraryJson
import munit.ScalaCheckSuite
import org.scalacheck.Prop

class JsonInterpolatorSuite extends ScalaCheckSuite {
  test("The json string interpolater should fail to compile with invalid JSON") {
    compileErrors("json\"\"\"1a2b3c\"\"\"")
  }

  test("The json string interpolater should work with top-level null") {
    assertEquals(json"null", Json.Null)
  }

  property("The json string interpolater should work with top-level booleans") {
    Prop.forAll { (value: Boolean) =>
      assertEquals(json"$value", Json.fromBoolean(value))
    }
  }

  property("The json string interpolater should work with top-level strings") {
    Prop.forAll { (value: String) =>
      assertEquals(json"$value", Json.fromString(value))
    }
  }

  property("The json string interpolater should work with top-level numbers") {
    Prop.forAll { (value: BigDecimal) =>
      assertEquals(json"$value", Json.fromBigDecimal(value))
    }
  }

  test("The json string interpolater should work with no interpolated variables") {
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

    assertEquals(parsed, Right(interpolated))
  }

  test(
    "The json string interpolater with interpolation in JSON value positions should work with interpolated variables"
  ) {
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

    assertEquals(parsed, Right(interpolated))
  }

  test(
    "The json string interpolater with interpolation in JSON value positions should work with interpolated literals"
  ) {
    val interpolated = json"""{ "k": ${1} }"""
    val parsed = parse(s"""{ "k": 1 }""")

    assertEquals(parsed, Right(interpolated))
  }

  test(
    "The json string interpolater with interpolation in JSON value positions should fail with unencodeable interpolated variables"
  ) {
    trait Foo
    val foo = new Foo {}

    compileErrors("json\"$foo\"")
  }

  property(
    "The json string interpolater with interpolation in JSON key positions should work with interpolated string variables"
  ) {
    Prop.forAll { (key: String, value: Json) =>
      val interpolated = json"{ $key: $value }"
      val escapedKey = Encoder[String].apply(key).noSpaces
      val parsed = parse(s"{ $escapedKey: ${value.noSpaces} }")

      assertEquals(parsed, Right(interpolated))
    }
  }

  property(
    "The json string interpolater with interpolation in JSON key positions should work with interpolated non-string variables"
  ) {
    Prop.forAll { (key: Int, value: Json) =>
      val interpolated = json"{ $key: $value }"
      val parsed = parse(s"""{ "${key.toString}": ${value.noSpaces} }""")

      assertEquals(parsed, Right(interpolated))
    }
  }

  test("The json string interpolater with interpolation in JSON key positions should work with interpolated literals") {
    val interpolated = json"""{ ${1}: "v" }"""
    val parsed = parse(s"""{ "1": "v" }""")

    assertEquals(parsed, Right(interpolated))
  }

  test(
    "The json string interpolater with interpolation in JSON key positions should fail with unencodeable interpolated variables"
  ) {
    trait Foo
    val foo = new Foo {}

    compileErrors("json\"{ $foo: 1 }\"")
  }
}
