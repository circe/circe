package io.circe

import org.scalatest.flatspec.AnyFlatSpec

class JsonSuite extends AnyFlatSpec {
  "Json#deepDropNullValues" should "remove null value for JsonObject" in {
    val actual = Json
      .fromFields(
        List(
          "a" -> Json.Null,
          "b" -> Json.fromString("c"),
          "d" -> Json.fromInt(1),
          "e" -> Json.True
        )
      )
      .deepDropNullValues

    assert(
      actual == Json.fromFields(
        List(
          "b" -> Json.fromString("c"),
          "d" -> Json.fromInt(1),
          "e" -> Json.True
        )
      )
    )
  }
  "Json#deepDropNullValues" should "remove null value for JsonArray" in {
    val actual = Json.fromValues(List(Json.Null, Json.fromString("a"))).deepDropNullValues

    assert(actual == Json.fromValues(List(Json.fromString("a"))))
  }
  "Json#deepDropNullValues" should "remove null value for nested object" in {
    val actual = Json
      .fromFields(
        List(
          "child1" -> Json.fromFields(List("a" -> Json.Null, "b" -> Json.fromString("c"))),
          "child2" -> Json.fromValues(List(Json.Null, Json.fromString("a")))
        )
      )
      .deepDropNullValues

    assert(
      actual == Json.fromFields(
        List(
          "child1" -> Json.fromFields(List("b" -> Json.fromString("c"))),
          "child2" -> Json.fromValues(List(Json.fromString("a")))
        )
      )
    )
  }
}
