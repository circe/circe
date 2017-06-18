package io.circe.literal

import io.circe.parser.parse
import io.circe.tests.CirceSuite
import shapeless.test.illTyped

class JsonInterpolatorSuite extends CirceSuite {
  "json" should "work with no interpolated variables" in {
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

  it should "work with interpolated variables" in {
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

    assert(parsed === Right(interpolated))
  }

  it should "work with interpolated strings as keys" in {
    val key = "foo"

    val interpolated = json"{ $key: 1 }"

    val parsed = parse("""{ "foo": 1 }""")

    assert(parsed === Right(interpolated))
  }

  it should "fail with invalid JSON" in {
    illTyped("json\"\"\"1a2b3c\"\"\"")
  }

  it should "fail with unencodeable interpolated variables" in {
    trait Foo

    val foo = new Foo {}

    illTyped("json\"$foo\"")
  }
}
