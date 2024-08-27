/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe

import cats.syntax.eq._
import io.circe.syntax._
import io.circe.tests.CirceMunitSuite
import org.scalacheck.Prop
import org.scalacheck.Prop._

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
      json.asBoolean ?= json.fold(None, Some(_), _ => None, _ => None, _ => None, _ => None)
    }
  }

  property("asNumber should give the same result as fold") {
    forAll { (json: Json) =>
      json.asNumber ?= json.fold(None, _ => None, Some(_), _ => None, _ => None, _ => None)
    }
  }

  property("asString should give the same result as fold") {
    forAll { (json: Json) =>
      json.asString ?= json.fold(None, _ => None, _ => None, Some(_), _ => None, _ => None)
    }
  }

  property("asArray should give the same result as fold") {
    forAll { (json: Json) =>
      json.asArray ?= json.fold(None, _ => None, _ => None, _ => None, Some(_), _ => None)
    }
  }

  property("asObject should give the same result as fold") {
    forAll { (json: Json) =>
      json.asObject ?= json.fold(None, _ => None, _ => None, _ => None, _ => None, Some(_))
    }
  }

  property("withNull should be identity with Json.Null") {
    forAll { (json: Json) =>
      json.withNull(Json.Null) ?= json
    }
  }

  property("withBoolean should be identity with Json.fromBoolean") {
    forAll { (json: Json) =>
      json.withBoolean(Json.fromBoolean) ?= json
    }
  }

  property("withNumber should be identity with Json.fromJsonNumber") {
    forAll { (json: Json) =>
      json.withNumber(Json.fromJsonNumber) ?= json
    }
  }

  property("withString should be identity with Json.fromString") {
    forAll { (json: Json) =>
      json.withString(Json.fromString) ?= json
    }
  }

  property("withArray should be identity with Json.fromValues") {
    forAll { (json: Json) =>
      json.withArray(Json.fromValues) ?= json
    }
  }

  property("withObject should be identity with Json.fromJsonObject") {
    forAll { (json: Json) =>
      json.withObject(Json.fromJsonObject) ?= json
    }
  }

  property("deepMerge should preserve argument order")(deepMergePreserveOrderProp)
  lazy val deepMergePreserveOrderProp = forAll { (js: List[Json]) =>
    val fields = js.zipWithIndex.map {
      case (j, i) => i.toString -> j
    }

    val reversed = Json.fromFields(fields.reverse)
    val merged = Json.fromFields(fields).deepMerge(reversed)

    merged.asObject.map(_.toList) ?= Some(fields.reverse)
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

    assertEquals(
      actual,
      Json.fromFields(
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

    assertEquals(
      actual,
      Json.fromFields(
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

    assertEquals(actual, Json.fromValues(List(Json.fromString("a"))))
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

    assertEquals(
      actual,
      Json.fromFields(
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

  test("fromStringOrNull return Null on None") {
    assertEquals(Json.fromStringOrNull(None), Json.Null)
  }

  test("fromStringOrNull return value on Some") {
    val s = "egg"
    assertEquals(Json.fromStringOrNull(Some(s)), Json.fromString(s))
  }

  test("fromBooleanOrNull return Null on None") {
    assertEquals(Json.fromBooleanOrNull(None), Json.Null)
  }

  property("fromBooleanOrNull return value on Some") {
    forAll { (value: Boolean) =>
      assertEquals(Json.fromBooleanOrNull(Some(value)), Json.fromBoolean(value))
    }
  }

  test("fromIntOrNull return Null on None") {
    assertEquals(Json.fromIntOrNull(None), Json.Null)
  }

  property("fromIntOrNull return value on Some") {
    forAll { (value: Int) =>
      assertEquals(Json.fromIntOrNull(Some(value)), Json.fromInt(value))
    }
  }

  test("fromLongOrNull return Null on None") {
    assertEquals(Json.fromLongOrNull(None), Json.Null)
  }

  property("fromLongOrNull return value on Some") {
    forAll { (value: Long) =>
      assertEquals(Json.fromLongOrNull(Some(value)), Json.fromLong(value))
    }
  }

  test("fromBigIntOrNull returns Null on None") {
    assertEquals(Json.fromBigIntOrNull(None), Json.Null)
  }

  property("fromBigDecimalOrNull return value on Some") {
    forAll { (value: BigDecimal) =>
      assertEquals(
        Json.fromBigDecimalOrNull(Some(value)),
        Json.fromBigDecimal(value)
      )
    }
  }

  test("fromBigDecimalOrNull return Null on None") {
    assertEquals(Json.fromBigDecimalOrNull(None), Json.Null)
  }

  test("fromDouble should fail on Double.NaN") {
    assertEquals(Json.fromDouble(Double.NaN), None)
  }

  test("fromDouble should fail on Double.PositiveInfinity") {
    assertEquals(Json.fromDouble(Double.PositiveInfinity), None)
  }

  test("fromDouble should fail on Double.NegativeInfinity") {
    assertEquals(Json.fromDouble(Double.NegativeInfinity), None)
  }

  test("fromDoubleOrNull should return Null on Double.NaN") {
    assertEquals(Json.fromDoubleOrNull(Double.NaN), Json.Null)
  }

  test("fromDoubleOrNull return Null on Double.PositiveInfinity") {
    assertEquals(Json.fromDoubleOrNull(Double.PositiveInfinity), Json.Null)
  }

  test("fromDoubleOrNull should return Null on Double.NegativeInfinity") {
    assertEquals(Json.fromDoubleOrNull(Double.NegativeInfinity), Json.Null)
  }

  test("fromDoubleOrNull should return Null on None") {
    assertEquals(Json.fromDoubleOrNull(None), Json.Null)
  }

  test("fromDoubleOrNull should return Null on some Double.NaN") {
    assertEquals(Json.fromDoubleOrNull(Some(Double.NaN)), Json.Null)
  }

  test("fromDoubleOrNull should return Null on some Double.PositiveInfinity") {
    assertEquals(Json.fromDoubleOrNull(Some(Double.PositiveInfinity)), Json.Null)
  }

  test("fromDoubleOrNull should return Null on some Double.NegativeInfinity") {
    assertEquals(Json.fromDoubleOrNull(Some(Double.NegativeInfinity)), Json.Null)
  }

  test("fromDoubleOrNull should return value") {
    assertEquals(Json.fromDoubleOrNull(Some(1.23)), Json.fromDoubleOrNull(1.23))
  }

  test("fromDoubleOrString should return String on Double.NaN") {
    assertEquals(Json.fromDoubleOrString(Double.NaN), Json.fromString("NaN"))
  }

  test("fromDoubleOrString return String on Double.PositiveInfinity") {
    assertEquals(Json.fromDoubleOrString(Double.PositiveInfinity), Json.fromString("Infinity"))
  }

  test("fromDoubleOrString should return String on Double.NegativeInfinity") {
    assertEquals(Json.fromDoubleOrString(Double.NegativeInfinity), Json.fromString("-Infinity"))
  }

  test("fromDoubleOrString should return JsonNumber Json values for valid Doubles") {
    assertEquals(Json.fromDoubleOrString(1.1), Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe("1.1")))
    assertEquals(Json.fromDoubleOrString(-1.2), Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe("-1.2")))
  }

  test("fromFloat should fail on Float.NaN") {
    assertEquals(Json.fromFloat(Float.NaN), None)
  }

  test("fromFloat should fail on Float.PositiveInfinity") {
    assertEquals(Json.fromFloat(Float.PositiveInfinity), None)
  }

  test("fromFloat should fail on Float.NegativeInfinity") {
    assertEquals(Json.fromFloat(Float.NegativeInfinity), None)
  }

  test("fromFloatOrNull should return Null on Float.NaN") {
    assertEquals(Json.fromFloatOrNull(Float.NaN), Json.Null)
  }

  test("fromFloatOrNull should return Null on Float.PositiveInfinity") {
    assertEquals(Json.fromFloatOrNull(Float.PositiveInfinity), Json.Null)
  }

  test("fromFloatOrNull should return Null on Float.NegativeInfinity") {
    assertEquals(Json.fromFloatOrNull(Float.NegativeInfinity), Json.Null)
  }

  test("fromFloatOrNull should return Null on None") {
    assertEquals(Json.fromFloatOrNull(None), Json.Null)
  }

  test("fromFloatOrNull should return Null on some Float.NaN") {
    assertEquals(Json.fromFloatOrNull(Some(Float.NaN)), Json.Null)
  }

  test("fromFloatOrNull should return Null on some Float.PositiveInfinity") {
    assertEquals(Json.fromFloatOrNull(Some(Float.PositiveInfinity)), Json.Null)
  }

  test("fromFloatOrNull should return Null on some Float.NegativeInfinity") {
    assertEquals(Json.fromFloatOrNull(Some(Float.NegativeInfinity)), Json.Null)
  }

  test("fromFloatOrNull should return value") {
    assertEquals(Json.fromFloatOrNull(Some(1.23f)), Json.fromFloatOrNull(1.23f))
  }

  test("fromFloatOrString should return String on Float.NaN") {
    assertEquals(Json.fromFloatOrString(Float.NaN), Json.fromString("NaN"))
  }

  test("fromFloatOrString should return String on Float.PositiveInfinity") {
    assertEquals(Json.fromFloatOrString(Float.PositiveInfinity), Json.fromString("Infinity"))
  }

  test("fromFloatOrString should return String on Float.NegativeInfinity") {
    assertEquals(Json.fromFloatOrString(Float.NegativeInfinity), Json.fromString("-Infinity"))
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
    assertEquals(actual, expected)
  }

  property("printer shortcuts should print the object")(printerShortcutsProp)
  lazy val printerShortcutsProp = forAll { (json: Json) =>
    json.noSpaces ?= Printer.noSpaces.print(json)
    json.spaces2 ?= Printer.spaces2.print(json)
    json.spaces4 ?= Printer.spaces4.print(json)
    json.noSpacesSortKeys ?= Printer.noSpacesSortKeys.print(json)
    json.spaces2SortKeys ?= Printer.spaces2SortKeys.print(json)
    json.spaces4SortKeys ?= Printer.spaces4SortKeys.print(json)
  }
}
