package io.circe

import io.circe.numbers.JsonNumber
import io.circe.syntax._
import io.circe.tests.CirceSuite

class JsonSuite extends CirceSuite with FloatJsonTests {
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

  """findAllByKey and its alias, \\"""  should "return all values matching the given key with key-value pairs at heights 0 and 1" in {
    val expected = List(Json.fromString(value1), Json.fromString(value2), Json.Null)
    val at0         = (key, Json.fromString(value1))
    val at1         = (key, Json.fromString(value2))
    val at1_2       = (key, Json.Null)
    val json        = Json.obj(at0, "y" -> Json.obj(at1), "z" -> Json.obj(at1_2))
    val result      = json.findAllByKey(key)
    val resultAlias = json \\ key

    assert(result === expected)
    assert(resultAlias === expected)
  }

  """findAllByKey and its alias, \\"""  should "return a List of a single, empty `JObject` for a `Json` with only that (key, value) matching" in {
    val expected    = List(Json.fromJsonObject(JsonObject.empty))
    val emptyJson   = (key, Json.fromJsonObject(JsonObject.empty))
    val json        = Json.obj(emptyJson)
    val result      = json.findAllByKey(key)
    val resultAlias = json \\ key

    assert(result === expected)
    assert(resultAlias === expected)
  }

  """findAllByKey and its alias, \\"""  should "return an empty List when used on a `Json` that's not a JSON array or object" in {
    val number  = Json.fromLong(42L)
    val string  = Json.fromString("foobar")
    val boolean = Json.fromBoolean(true)
    val `null`  = Json.Null

    val results      = List(number, string, boolean, `null`).map(json => json.findAllByKey("meaninglesskey"))
    val resultsAlias = List(number, string, boolean, `null`).map(json => json.\\("meaninglesskey"))
    assert(results.forall(_ == Nil))
    assert(resultsAlias.forall(_ == Nil))
  }

  val value3 = 42L

  """findAllByKey and its alias, \\"""  should "return all values matching the given key with key-value pairs at heights 0, 1, and 2" in {
    val expected    = List(Json.arr(Json.fromString(value1)), Json.fromString(value2), Json.fromLong(value3))
    val `0`         = (key, Json.arr(Json.fromString(value1)))
    val `1`         = (key, Json.fromString(value2))
    val `2`         = (key, Json.fromLong(value3))
    val json        = Json.obj(`0`, "y" -> Json.obj(`1`), "z" -> Json.obj(`2`))
    val result      = json.findAllByKey(key)
    val resultAlias = json \\ key

    assert(result === expected)
    assert(resultAlias === expected)
  }

  "fromDouble" should "fail on Double.NaN" in {
    assert(Json.fromDouble(Double.NaN) === None)
  }

  it should "fail on Double.PositiveInfinity" in {
    assert(Json.fromDouble(Double.PositiveInfinity) === None)
  }

  it should "fail on Double.NegativeInfinity" in {
    assert(Json.fromDouble(Double.NegativeInfinity) === None)
  }

  "fromDoubleOrNull" should "return Null on Double.NaN" in {
    assert(Json.fromDoubleOrNull(Double.NaN) === Json.Null)
  }

  it should "return Null on Double.PositiveInfinity" in {
    assert(Json.fromDoubleOrNull(Double.PositiveInfinity) === Json.Null)
  }

  it should "return Null on Double.NegativeInfinity" in {
    assert(Json.fromDoubleOrNull(Double.NegativeInfinity) === Json.Null)
  }

  "fromDoubleOrString" should "return String on Double.NaN" in {
    assert(Json.fromDoubleOrString(Double.NaN) === Json.JString("NaN"))
  }

  it should "return String on Double.PositiveInfinity" in {
    assert(Json.fromDoubleOrString(Double.PositiveInfinity) === Json.JString("Infinity"))
  }

  it should "return String on Double.NegativeInfinity" in {
    assert(Json.fromDoubleOrString(Double.NegativeInfinity) === Json.JString("-Infinity"))
  }

  it should "return JNumber for valid Doubles" in {
    assert(Json.fromDoubleOrString(1.1) === Json.fromJsonNumber(JsonNumber.parseJsonNumberUnsafe("1.1")))
    assert(Json.fromDoubleOrString(-1.2) === Json.fromJsonNumber(JsonNumber.parseJsonNumberUnsafe("-1.2")))
  }

  "fromFloat" should "fail on Float.NaN" in {
    assert(Json.fromFloat(Float.NaN) === None)
  }

  it should "fail on Float.PositiveInfinity" in {
    assert(Json.fromFloat(Float.PositiveInfinity) === None)
  }

  it should "fail on Float.NegativeInfinity" in {
    assert(Json.fromFloat(Float.NegativeInfinity) === None)
  }

  "fromFloatOrNull" should "return Null on Float.NaN" in {
    assert(Json.fromFloatOrNull(Float.NaN) === Json.Null)
  }

  it should "return Null on Float.PositiveInfinity" in {
    assert(Json.fromFloatOrNull(Float.PositiveInfinity) === Json.Null)
  }

  it should "return Null on Float.NegativeInfinity" in {
    assert(Json.fromFloatOrNull(Float.NegativeInfinity) === Json.Null)
  }

  "fromFloatOrString" should "return String on Float.NaN" in {
    assert(Json.fromFloatOrString(Float.NaN) === Json.JString("NaN"))
  }

  it should "return String on Float.PositiveInfinity" in {
    assert(Json.fromFloatOrString(Float.PositiveInfinity) === Json.JString("Infinity"))
  }

  it should "return String on Float.NegativeInfinity" in {
    assert(Json.fromFloatOrString(Float.NegativeInfinity) === Json.JString("-Infinity"))
  }

  "obj" should "create object fluently" in {
    val actual = Json.obj(
      "a" := 1,
      "b" := "asdf",
      "c" := Seq(1, 2, 3)
    )
    val expected = Json.obj(
      ("a", Json.fromLong(1L)),
      ("b", Json.fromString("asdf")),
      ("c", Json.arr(Json.fromLong(1L), Json.fromLong(2L), Json.fromLong(3L)))
    )
    assert(actual === expected)
  }
}
