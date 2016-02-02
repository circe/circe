package io.circe.literal

import cats.data.Xor
import io.circe.Json
import io.circe.parser.parse
import io.circe.tests.CirceSuite
import shapeless.test.illTyped

class JsonInterpolatorSuite extends CirceSuite {
  test("The json interpolator should work with no interpolated variables") {
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

    assert(parsed === Xor.right(interpolated))
  }

  test("The json interpolator should work with interpolated variables") {
    val i = 13
    val m = Map("bar" -> List(1, 2, 3), "baz" -> Nil)

    val interpolated = json"""{ "i": $i, "ms": [$m, $m], "other": [1.0, "abc"] }"""

    val parsed = parse("""
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

    assert(parsed === Xor.right(interpolated))
  }

  test("The json interpolator should work with interpolated strings as keys") {
    val key = "foo"

    val interpolated = json"{ $key: 1 }"

    val parsed = parse("""{ "foo": 1 }""")

    assert(parsed === Xor.right(interpolated))
  }


  test("The json interpolator should fail with invalid JSON") {
    illTyped("json\"\"\"1a2b3c\"\"\"")
  }

  test("The json interpolator should fail with unencodable interpolated variables") {
    trait Foo

    val foo = new Foo {}

    illTyped("json\"$foo\"")
  }
}
