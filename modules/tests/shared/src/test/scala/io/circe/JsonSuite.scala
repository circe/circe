package io.circe

import io.circe.Json.{JString, JArray, JNumber, JBoolean, JObject, JNull}
import io.circe.tests.CirceSuite

class JsonSuite extends CirceSuite {
  "deepMerge" should "preserve argument order" in forAll { (js: List[Json]) =>
    val fields = js.zipWithIndex.map {
      case (j, i) => i.toString -> j
    }

    val reversed = Json.fromFields(fields.reverse)
    val merged = Json.fromFields(fields).deepMerge(reversed)

    assert(merged.asObject.map(_.toList) === Some(fields.reverse))
  }

  val key    = "x"
  val value1 = "fizz"
  val value2 = "foobar"

  """findAllByKey and its alias, \\"""  should "return all values matching the given key with key-value pairs at heights 0 and 1." in {
    val expected = List(JString(value1), JString(value2), JNull)
    val at0         = (key, JString(value1))
    val at1         = (key, JString(value2))
    val at1_2       = (key, JNull)
    val json        = Json.obj(at0, "y" -> Json.obj(at1), "z" -> Json.obj(at1_2))
    val result      = json.findAllByKey(key)
    val resultAlias = json \\ key

    assert(result === expected && resultAlias === expected)
  }

  """findAllByKey and its alias, \\"""  should "return a List of a single, empty `JObject` for a `Json` with only that (key, value) matching." in {
    val expected    = List(JObject(JsonObject.empty))
    val emptyJson   = (key, JObject(JsonObject.empty))
    val json        = Json.obj(emptyJson)
    val result      = json.findAllByKey(key)
    val resultAlias = json \\ key

    assert(result === expected && resultAlias === expected)
  }

  """findAllByKey and its alias, \\"""  should "return an empty List when used on a `Json` that's not a `JArray` or `JObject`" in {
    val number  = JNumber(JsonLong(42L))
    val string  = JString("foobar")
    val boolean = JBoolean(true)
    val `null`  = JNull

    val results      = List(number, string, boolean, `null`).map(json => json.findAllByKey("meaninglesskey"))
    val resultsAlias = List(number, string, boolean, `null`).map(json => json.\\("meaninglesskey"))
    assert(results.forall(_ == Nil))
    assert(resultsAlias.forall(_ == Nil))
  }

  val value3 = 42L

  """findAllByKey and its alias, \\"""  should "return all values matching the given key with key-value pairs at heights  0, 1, and 2." in {
    val expected    = List(JArray(List(JString(value1))), JString(value2), JNumber(JsonLong(value3)))
    val `0`         = (key, JArray(List(JString(value1))))
    val `1`         = (key, JString(value2))
    val `2`         = (key, JNumber(JsonLong(value3)))
    val json        = Json.obj(`0`, "y" -> Json.obj(`1`), "z" -> Json.obj(`2`))
    val result      = json.findAllByKey(key)
    val resultAlias = json \\ key

    assert(result === expected && resultAlias === expected)
  }
}
