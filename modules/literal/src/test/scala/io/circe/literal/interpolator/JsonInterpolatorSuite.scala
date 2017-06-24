package io.circe.literal.interpolator

import io.circe.Json
import io.circe.literal.JsonStringContext
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

  it should "work with interpolated literals" in {
    val interpolated = json"""{ "k": ${ 1 } }"""
    val parsed = parse(s"""{ "k": 1 }""")

    assert(parsed === Right(interpolated))
  }

  it should "work with interpolated strings as keys" in forAll { (key: String, value: Json) =>
    val interpolated = json"{ $key: $value }"
    val escapedKey = key.replaceAll("\"", "\\\"")
    val parsed = parse(s"""{ "$escapedKey": ${ value.noSpaces } }""")

    assert(parsed === Right(interpolated))
  }

  it should "work with interpolated non-strings as keys" in forAll { (key: Int, value: Json) =>
    val interpolated = json"{ $key: $value }"
    val parsed = parse(s"""{ "${ key.toString }": ${ value.noSpaces } }""")

    assert(parsed === Right(interpolated))
  }

  it should "work with interpolated literals as keys" in {
    val interpolated = json"""{ ${ 1 }: "v" }"""
    val parsed = parse(s"""{ "1": "v" }""")

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
