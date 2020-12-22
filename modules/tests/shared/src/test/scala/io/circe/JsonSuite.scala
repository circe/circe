package io.circe

import cats.instances.all._
import cats.syntax.eq._
import io.circe.syntax._
import io.circe.tests.CirceMunitSuite
import org.scalacheck.Prop
import org.scalacheck.Prop.forAll

class JsonSuite extends CirceMunitSuite with FloatJsonTests {

  property("foldWith should give the same result as fold")(foldWithProp)

  lazy val foldWithProp: Prop = Prop.forAll { (json: Json) =>
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

  property("asNull should give the same result as fold")(asNullProp)

  lazy val asNullProp: Prop = Prop.forAll { (json: Json) =>
    assert(json.asNull === json.fold(Some(()), _ => None, _ => None, _ => None, _ => None, _ => None))
  }

  property("asBoolean should give the same result as fold") {
    forAll { (json: Json) =>
      assert(json.asBoolean === json.fold(None, Some(_), _ => None, _ => None, _ => None, _ => None))
    }
  }

  property("asNumber should give the same result as fold") {
    forAll { (json: Json) =>
      assert(json.asNumber === json.fold(None, _ => None, Some(_), _ => None, _ => None, _ => None))
    }
  }

  property("asString should give the same result as fold") {
    forAll { (json: Json) =>
      assert(json.asString === json.fold(None, _ => None, _ => None, Some(_), _ => None, _ => None))
    }
  }

  property("asArray should give the same result as fold") {
    forAll { (json: Json) =>
      assert(json.asArray === json.fold(None, _ => None, _ => None, _ => None, Some(_), _ => None))
    }
  }

  property("asObject should give the same result as fold") {
    forAll { (json: Json) =>
      assert(json.asObject === json.fold(None, _ => None, _ => None, _ => None, _ => None, Some(_)))
    }
  }

  property("withNull should be identity with Json.Null") {
    forAll { (json: Json) =>
      assert(json.withNull(Json.Null) === json)
    }
  }

  property("withBoolean should be identity with Json.fromBoolean") {
    forAll { (json: Json) =>
      assert(json.withBoolean(Json.fromBoolean) === json)
    }
  }

  property("withNumber should be identity with Json.fromJsonNumber") {
    forAll { (json: Json) =>
      assert(json.withNumber(Json.fromJsonNumber) === json)
    }
  }

  property("withString should be identity with Json.fromString") {
    forAll { (json: Json) =>
      assert(json.withString(Json.fromString) === json)
    }
  }

  property("withArray should be identity with Json.fromValues") {
    forAll { (json: Json) =>
      assert(json.withArray(Json.fromValues) === json)
    }
  }

  property("withObject should be identity with Json.fromJsonObject") {
    forAll { (json: Json) =>
      assert(json.withObject(Json.fromJsonObject) === json)
    }
  }

  property("deepMerge should preserve argument order")(deepMergePreserveOrderProp)
  lazy val deepMergePreserveOrderProp = forAll { (js: List[Json]) =>
    val fields = js.zipWithIndex.map {
      case (j, i) => i.toString -> j
    }

    val reversed = Json.fromFields(fields.reverse)
    val merged = Json.fromFields(fields).deepMerge(reversed)

    assert(merged.asObject.map(_.toList) === Some(fields.reverse))
  }

  test("dropEmptyValues should remove empty values for JsonObject") {
    val actual = Json
      .fromFields(
        List(
          "a" -> Json.fromFields(Nil),
          "b" -> Json.fromValues(Nil),
          "c" -> Json.fromValues(List(Json.fromInt(1))),
          "d" -> Json.fromFields(List("a" -> Json.fromInt(1))),
          "e" -> Json.fromInt(1)
        )
      )
      .dropEmptyValues

    assert(
      actual == Json.fromFields(
        List(
          "c" -> Json.fromValues(List(Json.fromInt(1))),
          "d" -> Json.fromFields(List("a" -> Json.fromInt(1))),
          "e" -> Json.fromInt(1)
        )
      )
    )
  }

  test("deepDropNullValues should remove null value for JsonObject") {
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

  test("deepDropNullValues should remove null value for JsonArray") {
    val actual = Json.fromValues(List(Json.Null, Json.fromString("a"))).deepDropNullValues

    assert(actual == Json.fromValues(List(Json.fromString("a"))))
  }

  test("deepDropNullValues should remove null value for nested object") {
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

  val key = "x"
  val value1 = "fizz"
  val value2 = "foobar"

  test(
    """findAllByKey and its alias, should return all values matching the given key with key-value pairs at heights 0 and 1."""
  ) {
    val expected = List(Json.fromString(value1), Json.fromString(value2), Json.Null)
    val at0 = (key, Json.fromString(value1))
    val at1 = (key, Json.fromString(value2))
    val at1_2 = (key, Json.Null)
    val json = Json.obj(at0, "y" -> Json.obj(at1), "z" -> Json.obj(at1_2))
    val result = json.findAllByKey(key)
    val resultAlias = json \\ key

    assert(result === expected && resultAlias === expected)
  }

  test(
    """findAllByKey and its alias, \\ should return a List of a single, empty object for a Json value with only that (key, value) matching."""
  ) {
    val expected = List(Json.fromJsonObject(JsonObject.empty))
    val emptyJson = (key, Json.fromJsonObject(JsonObject.empty))
    val json = Json.obj(emptyJson)
    val result = json.findAllByKey(key)
    val resultAlias = json \\ key

    assert(result === expected && resultAlias === expected)
  }

  test(
    """findAllByKey and its alias, \\"" should return an empty List when used on a Json that's not an array or object"""
  ) {
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

  test(
    """findAllByKey and its alias, \\"" should return all values matching the given key with key-value pairs at heights  0, 1, and 2."""
  ) {
    val expected = List(Json.arr(Json.fromString(value1)), Json.fromString(value2), Json.fromLong(value3))
    val `0` = (key, Json.arr(Json.fromString(value1)))
    val `1` = (key, Json.fromString(value2))
    val `2` = (key, Json.fromLong(value3))
    val json = Json.obj(`0`, "y" -> Json.obj(`1`), "z" -> Json.obj(`2`))
    val result = json.findAllByKey(key)
    val resultAlias = json \\ key

    assert(result === expected && resultAlias === expected)
  }

  test("fromDouble should fail on Double.NaN") {
    assert(Json.fromDouble(Double.NaN) === None)
  }

  test("fromDouble should fail on Double.PositiveInfinity") {
    assert(Json.fromDouble(Double.PositiveInfinity) === None)
  }

  test("fromDouble should fail on Double.NegativeInfinity") {
    assert(Json.fromDouble(Double.NegativeInfinity) === None)
  }

  test("fromDoubleOrNull should return Null on Double.NaN") {
    assert(Json.fromDoubleOrNull(Double.NaN) === Json.Null)
  }

  test("fromDoubleOrNull return Null on Double.PositiveInfinity") {
    assert(Json.fromDoubleOrNull(Double.PositiveInfinity) === Json.Null)
  }

  test("fromDoubleOrNull should return Null on Double.NegativeInfinity") {
    assert(Json.fromDoubleOrNull(Double.NegativeInfinity) === Json.Null)
  }

  test("fromDoubleOrString should return String on Double.NaN") {
    assert(Json.fromDoubleOrString(Double.NaN) === Json.fromString("NaN"))
  }

  test("fromDoubleOrString return String on Double.PositiveInfinity") {
    assert(Json.fromDoubleOrString(Double.PositiveInfinity) === Json.fromString("Infinity"))
  }

  test("fromDoubleOrString should return String on Double.NegativeInfinity") {
    assert(Json.fromDoubleOrString(Double.NegativeInfinity) === Json.fromString("-Infinity"))
  }

  test("fromDoubleOrString should return JsonNumber Json values for valid Doubles") {
    assert(Json.fromDoubleOrString(1.1) === Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe("1.1")))
    assert(Json.fromDoubleOrString(-1.2) === Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe("-1.2")))
  }

  test("fromFloat should fail on Float.NaN") {
    assert(Json.fromFloat(Float.NaN) === None)
  }

  test("fromFloat should fail on Float.PositiveInfinity") {
    assert(Json.fromFloat(Float.PositiveInfinity) === None)
  }

  test("fromFloat should fail on Float.NegativeInfinity") {
    assert(Json.fromFloat(Float.NegativeInfinity) === None)
  }

  test("fromFloatOrNull should return Null on Float.NaN") {
    assert(Json.fromFloatOrNull(Float.NaN) === Json.Null)
  }

  test("fromFloatOrNull should return Null on Float.PositiveInfinity") {
    assert(Json.fromFloatOrNull(Float.PositiveInfinity) === Json.Null)
  }

  test("fromFloatOrNull should return Null on Float.NegativeInfinity") {
    assert(Json.fromFloatOrNull(Float.NegativeInfinity) === Json.Null)
  }

  test("fromFloatOrString should return String on Float.NaN") {
    assert(Json.fromFloatOrString(Float.NaN) === Json.fromString("NaN"))
  }

  test("fromFloatOrString should return String on Float.PositiveInfinity") {
    assert(Json.fromFloatOrString(Float.PositiveInfinity) === Json.fromString("Infinity"))
  }

  test("fromFloatOrString should return String on Float.NegativeInfinity") {
    assert(Json.fromFloatOrString(Float.NegativeInfinity) === Json.fromString("-Infinity"))
  }

  test("obj should create object fluently") {
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

  property("printer shortcuts should print the object")(printerShortcutsProp)
  lazy val printerShortcutsProp = forAll { (json: Json) =>
    assert(json.noSpaces === Printer.noSpaces.print(json))
    assert(json.spaces2 === Printer.spaces2.print(json))
    assert(json.spaces4 === Printer.spaces4.print(json))
    assert(json.noSpacesSortKeys === Printer.noSpacesSortKeys.print(json))
    assert(json.spaces2SortKeys === Printer.spaces2SortKeys.print(json))
    assert(json.spaces4SortKeys === Printer.spaces4SortKeys.print(json))
  }
}
