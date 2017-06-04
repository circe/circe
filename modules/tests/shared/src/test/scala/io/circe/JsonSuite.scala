package io.circe

import io.circe.Json.{JString, JArray, JNumber, JBoolean, JObject, JNull}
import io.circe.syntax._
import io.circe.tests.CirceSuite

class JsonSuite extends CirceSuite with FloatJsonTests {
  "foldWith" should "give the same value as fold" in forAll { (json: Json) =>
    val z: Int = 0
    val b: Boolean => Int = if (_) 1 else 2
    val n: JsonNumber => Int = _.truncateToInt
    val s: String => Int = _.length
    val a: Vector[Json] => Int = _.size
    val o: JsonObject => Int = _.fields.size

    val result = json.foldWith(
      new Json.Folder[Int] {
        def onNull: Int = z
        def onBoolean(value: Boolean): Int = b(value)
        def onNumber(value: JsonNumber): Int = n(value)
        def onString(value: String): Int = s(value)
        def onArray(value: Vector[Json]): Int = a(value)
        def onObject(value: JsonObject): Int = o(value)
      }
    )

    assert(result === json.fold(z, b, n, s, a, o))
  }

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
    val expected    = List(JArray(Vector(JString(value1))), JString(value2), JNumber(JsonLong(value3)))
    val `0`         = (key, JArray(Vector(JString(value1))))
    val `1`         = (key, JString(value2))
    val `2`         = (key, JNumber(JsonLong(value3)))
    val json        = Json.obj(`0`, "y" -> Json.obj(`1`), "z" -> Json.obj(`2`))
    val result      = json.findAllByKey(key)
    val resultAlias = json \\ key

    assert(result === expected && resultAlias === expected)
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
    assert(Json.fromDoubleOrString(1.1) === Json.JNumber(JsonNumber.fromDecimalStringUnsafe("1.1")))
    assert(Json.fromDoubleOrString(-1.2) === Json.JNumber(JsonNumber.fromDecimalStringUnsafe("-1.2")))
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
    val expected = JObject(JsonObject(
      ("a", JNumber(JsonLong(1))),
      ("b", JString("asdf")),
      ("c", JArray(Vector(JNumber(JsonLong(1)), JNumber(JsonLong(2)), JNumber(JsonLong(3)))))
    ))
    assert(actual === expected)
  }
}
