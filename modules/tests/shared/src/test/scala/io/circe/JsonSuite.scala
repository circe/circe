package io.circe

import io.circe.syntax._
import io.circe.tests.CirceSuite

class JsonSuite extends CirceSuite with FloatJsonTests {
  "foldWith" should "give the same result as fold" in forAll { (json: Json) =>
    val z: Int = 0
    val b: Boolean => Int = if (_) 1 else 2
    val n: JsonNumber => Int = _.toDouble.toInt
    val s: String => Int = _.length
    val a: Vector[Json] => Int = _.size
    val o: JsonObject => Int = _.keys.size

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

  "asNull" should "give the same result as fold" in forAll { (json: Json) =>
    assert(json.asNull === json.fold(Some(()), _ => None, _ => None, _ => None, _ => None, _ => None))
  }

  "asBoolean" should "give the same result as fold" in forAll { (json: Json) =>
    assert(json.asBoolean === json.fold(None, Some(_), _ => None, _ => None, _ => None, _ => None))
  }

  "asNumber" should "give the same result as fold" in forAll { (json: Json) =>
    assert(json.asNumber === json.fold(None, _ => None, Some(_), _ => None, _ => None, _ => None))
  }

  "asString" should "give the same result as fold" in forAll { (json: Json) =>
    assert(json.asString === json.fold(None, _ => None, _ => None, Some(_), _ => None, _ => None))
  }

  "asArray" should "give the same result as fold" in forAll { (json: Json) =>
    assert(json.asArray === json.fold(None, _ => None, _ => None, _ => None, Some(_), _ => None))
  }

  "asObject" should "give the same result as fold" in forAll { (json: Json) =>
    assert(json.asObject === json.fold(None, _ => None, _ => None, _ => None, _ => None, Some(_)))
  }

  "withNull" should "be identity with Json.Null" in forAll { (json: Json) =>
    assert(json.withNull(Json.Null) === json)
  }

  "withBoolean" should "be identity with Json.fromBoolean" in forAll { (json: Json) =>
    assert(json.withBoolean(Json.fromBoolean) === json)
  }

  "withNumber" should "be identity with Json.fromJsonNumber" in forAll { (json: Json) =>
    assert(json.withNumber(Json.fromJsonNumber) === json)
  }

  "withString" should "be identity with Json.fromString" in forAll { (json: Json) =>
    assert(json.withString(Json.fromString) === json)
  }

  "withArray" should "be identity with Json.fromValues" in forAll { (json: Json) =>
    assert(json.withArray(Json.fromValues) === json)
  }

  "withObject" should "be identity with Json.fromJsonObject" in forAll { (json: Json) =>
    assert(json.withObject(Json.fromJsonObject) === json)
  }

  "deepMerge" should "preserve argument order" in forAll { (js: List[Json]) =>
    val fields = js.zipWithIndex.map {
      case (j, i) => i.toString -> j
    }

    val reversed = Json.fromFields(fields.reverse)
    val merged = Json.fromFields(fields).deepMerge(reversed)

    assert(merged.asObject.map(_.toList) === Some(fields.reverse))
  }
  "canonicallyEqual" should "not consider null values" in {
    val expected = true
    val this_json = Json.fromJsonObject(JsonObject.empty)
    val that_json = Json.obj(("foo", Json.Null))
    val actual = this_json.canonicallyEqual(that_json)
    assert(actual == expected)
  }

  it should "not consider field ordering" in {
    val expected = true
    val this_json = Json.obj("a" -> Json.fromInt(1), "b" -> Json.fromInt(2))
    val that_json = Json.obj("b" -> Json.fromInt(2), "a" -> Json.fromInt(1))
    val actual = this_json.canonicallyEqual(that_json)
    assert(actual == expected)
  }

  val key = "x"
  val value1 = "fizz"
  val value2 = "foobar"

  """findAllByKey and its alias, \\""" should "return all values matching the given key with key-value pairs at heights 0 and 1." in {
    val expected = List(Json.fromString(value1), Json.fromString(value2), Json.Null)
    val at0 = (key, Json.fromString(value1))
    val at1 = (key, Json.fromString(value2))
    val at1_2 = (key, Json.Null)
    val json = Json.obj(at0, "y" -> Json.obj(at1), "z" -> Json.obj(at1_2))
    val result = json.findAllByKey(key)
    val resultAlias = json \\ key

    assert(result === expected && resultAlias === expected)
  }

  """findAllByKey and its alias, \\""" should "return a List of a single, empty object for a Json value with only that (key, value) matching." in {
    val expected = List(Json.fromJsonObject(JsonObject.empty))
    val emptyJson = (key, Json.fromJsonObject(JsonObject.empty))
    val json = Json.obj(emptyJson)
    val result = json.findAllByKey(key)
    val resultAlias = json \\ key

    assert(result === expected && resultAlias === expected)
  }

  """findAllByKey and its alias, \\""" should "return an empty List when used on a Json that's not an array or object" in {
    val number = Json.fromLong(42L)
    val string = Json.fromString("foobar")
    val boolean = Json.fromBoolean(true)
    val `null` = Json.Null

    val results = List(number, string, boolean, `null`).map(json => json.findAllByKey("meaninglesskey"))
    val resultsAlias = List(number, string, boolean, `null`).map(json => json.\\("meaninglesskey"))
    assert(results.forall(_ == Nil))
    assert(resultsAlias.forall(_ == Nil))
  }

  val value3 = 42L

  """findAllByKey and its alias, \\""" should "return all values matching the given key with key-value pairs at heights  0, 1, and 2." in {
    val expected = List(Json.arr(Json.fromString(value1)), Json.fromString(value2), Json.fromLong(value3))
    val `0` = (key, Json.arr(Json.fromString(value1)))
    val `1` = (key, Json.fromString(value2))
    val `2` = (key, Json.fromLong(value3))
    val json = Json.obj(`0`, "y" -> Json.obj(`1`), "z" -> Json.obj(`2`))
    val result = json.findAllByKey(key)
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
    assert(Json.fromDoubleOrString(Double.NaN) === Json.fromString("NaN"))
  }

  it should "return String on Double.PositiveInfinity" in {
    assert(Json.fromDoubleOrString(Double.PositiveInfinity) === Json.fromString("Infinity"))
  }

  it should "return String on Double.NegativeInfinity" in {
    assert(Json.fromDoubleOrString(Double.NegativeInfinity) === Json.fromString("-Infinity"))
  }

  it should "return JsonNumber Json values for valid Doubles" in {
    assert(Json.fromDoubleOrString(1.1) === Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe("1.1")))
    assert(Json.fromDoubleOrString(-1.2) === Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe("-1.2")))
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
    assert(Json.fromFloatOrString(Float.NaN) === Json.fromString("NaN"))
  }

  it should "return String on Float.PositiveInfinity" in {
    assert(Json.fromFloatOrString(Float.PositiveInfinity) === Json.fromString("Infinity"))
  }

  it should "return String on Float.NegativeInfinity" in {
    assert(Json.fromFloatOrString(Float.NegativeInfinity) === Json.fromString("-Infinity"))
  }

  "obj" should "create object fluently" in {
    val actual = Json.obj(
      "a" := 1L,
      "b" := "asdf",
      "c" := Seq(1L, 2L, 3L)
    )
    val expected = Json.fromJsonObject(
      JsonObject(
        ("a", Json.fromLong(1)),
        ("b", Json.fromString("asdf")),
        ("c", Json.arr(Json.fromLong(1L), Json.fromLong(2L), Json.fromLong(3L)))
      )
    )
    assert(actual === expected)
  }

  "printer shortcuts" should "print the object" in forAll { (json: Json) =>
    assert(json.noSpaces === Printer.noSpaces.pretty(json))
    assert(json.spaces2 === Printer.spaces2.pretty(json))
    assert(json.spaces4 === Printer.spaces4.pretty(json))
    assert(json.noSpacesSortKeys === Printer.noSpacesSortKeys.pretty(json))
    assert(json.spaces2SortKeys === Printer.spaces2SortKeys.pretty(json))
    assert(json.spaces4SortKeys === Printer.spaces4SortKeys.pretty(json))
  }
}
