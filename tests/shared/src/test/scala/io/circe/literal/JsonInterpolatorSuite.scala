package io.circe.literal

import cats.data.Xor
import io.circe.Json
import io.circe.parse.parse
import io.circe.tests.CirceSuite
import shapeless.test.illTyped

class JsonInterpolatorSuite extends CirceSuite {
  test("The json interpolator should work with no interpolated variables") {
    val interpolated = json"""
    {
      "a": [1, 2, 3],
      "b": { "foo": false },
      "c": null
    }
    """

    val parsed = parse(
      """
      {
        "a": [1, 2, 3],
        "b": { "foo": false },
        "c": null
      }
      """
    )

    assert(parsed === Xor.right(interpolated))
  }

  test("The json interpolator should work with interpolated variables") {
    val i = 13
    val m = Map("bar" -> List(1, 2, 3), "baz" -> Nil)

    val interpolated = json"""{ "i": $i, "ms": [$m, $m] }"""

    val parsed = parse("""
      {
        "i": 13,
        "ms": [
          { "bar": [1, 2, 3], "baz": [] },
          { "bar": [1, 2, 3], "baz": [] }
        ]
      }
      """
    )

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
