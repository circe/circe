package io.circe

import cats.data.Const
import cats.kernel.Eq
import cats.syntax.eq._
import io.circe.tests.CirceSuite
import org.scalatest.exceptions.TestFailedException

class JsonObjectSuite extends CirceSuite {
  "JsonObject.fromIterable" should "drop all but the last instance when fields have the same key" in {
    forAll { (key: String, values: List[Json]) =>
      val result = JsonObject.fromIterable(values.map(key -> _))
      val expected = if (values.isEmpty) JsonObject.empty else JsonObject.singleton(key, values.last)

      assert(result === expected)
    }
  }

  it should "maintain the first position when keys are duplicated" in {
    val fields = List("a" -> Json.fromInt(0), "b" -> Json.fromInt(1), "c" -> Json.fromInt(2), "b" -> Json.fromInt(3))
    val expected = JsonObject("a" -> Json.fromInt(0), "b" -> Json.fromInt(3), "c" -> Json.fromInt(2))

    assert(JsonObject.fromIterable(fields) === expected)
  }

  "JsonObject.fromFoldable" should "match JsonObject.fromIterable" in { (fields: List[(String, Json)]) =>
    val result1 = JsonObject.fromIterable(fields)
    val result2 = JsonObject.fromFoldable(fields)

    assert(result1.hashCode === result2.hashCode)
    assert(result1 === result2)
  }

  it should "drop all but the last instance when fields have the same key" in {
    forAll { (key: String, values: List[Json]) =>
      val result = JsonObject.fromFoldable(values.map(key -> _))
      val expected = if (values.isEmpty) JsonObject.empty else JsonObject.singleton(key, values.last)

      assert(result === expected)
    }
  }

  it should "maintain the first position when keys are duplicated" in {
    val fields = List("a" -> Json.fromInt(0), "b" -> Json.fromInt(1), "c" -> Json.fromInt(2), "b" -> Json.fromInt(3))
    val expected = JsonObject("a" -> Json.fromInt(0), "b" -> Json.fromInt(3), "c" -> Json.fromInt(2))

    assert(JsonObject.fromFoldable(fields) === expected)
  }

  "JsonObject.apply" should "match JsonObject.fromIterable" in { (fields: List[(String, Json)]) =>
    assert(JsonObject(fields: _*) === JsonObject.fromIterable(fields))
  }

  "apply" should "find fields if they exist" in { (fields: List[(String, Json)], key: String) =>
    val result1 = JsonObject.fromIterable(fields)
    val result2 = JsonObject.fromFoldable(fields)
    val expected = fields.find(_._1 == key).map(_._2)

    assert(result1(key) === expected)
    assert(result2(key) === expected)
  }

  "contains" should "find fields if they exist" in { (fields: List[(String, Json)], key: String) =>
    val result1 = JsonObject.fromIterable(fields)
    val result2 = JsonObject.fromFoldable(fields)
    val expected = fields.find(_._1 == key).nonEmpty

    assert(result1.contains(key) === expected)
    assert(result2.contains(key) === expected)
  }

  "size" should "return the expected result" in { (fields: List[(String, Json)]) =>
    val result1 = JsonObject.fromIterable(fields)
    val result2 = JsonObject.fromFoldable(fields)
    val expected = fields.toMap.size

    assert(result1.size === expected)
    assert(result2.size === expected)
  }

  "isEmpty" should "return the expected result" in { (fields: List[(String, Json)]) =>
    val result1 = JsonObject.fromIterable(fields)
    val result2 = JsonObject.fromFoldable(fields)
    val expected = fields.isEmpty

    assert(result1.isEmpty === expected)
    assert(result2.isEmpty === expected)
  }

  "nonEmpty" should "return the expected result" in { (fields: List[(String, Json)]) =>
    val result1 = JsonObject.fromIterable(fields)
    val result2 = JsonObject.fromFoldable(fields)
    val expected = fields.nonEmpty

    assert(result1.nonEmpty === expected)
    assert(result2.nonEmpty === expected)
  }

  "kleisli" should "find fields if they exist" in { (fields: List[(String, Json)], key: String) =>
    val result1 = JsonObject.fromIterable(fields)
    val result2 = JsonObject.fromFoldable(fields)
    val expected = fields.find(_._1 == key).map(_._2)

    assert(result1.kleisli(key) === expected)
    assert(result2.kleisli(key) === expected)
  }

  "keys" should "return all keys" in forAll { (fields: List[(String, Json)]) =>
    val result1 = JsonObject.fromIterable(fields)
    val result2 = JsonObject.fromFoldable(fields)
    val expected = fields.map(_._1).distinct

    assert(result1.keys.toList === expected)
    assert(result2.keys.toList === expected)
  }

  "values" should "return all values" in forAll { (fields: List[(String, Json)]) =>
    val result1 = JsonObject.fromIterable(fields)
    val result2 = JsonObject.fromFoldable(fields)
    val expected: List[Json] = fields
      .foldLeft(List.empty[(String, Json)]) {
        case (acc, (key, value)) =>
          val index = acc.indexWhere(_._1 == key)

          if (index < 0) {
            acc :+ (key -> value)
          } else {
            acc.updated(index, (key, value))
          }
      }
      .map(_._2)

    assert(result1.values.toList === expected)
    assert(result2.values.toList === expected)
  }

  "toMap" should "round-trip through JsonObject.fromMap" in forAll { (fields: List[(String, Json)]) =>
    val result1 = JsonObject.fromIterable(fields)
    val result2 = JsonObject.fromFoldable(fields)
    val expected = result1

    assert(JsonObject.fromMap(result1.toMap) === expected)
    assert(JsonObject.fromMap(result2.toMap) === expected)
  }

  "toIterable" should "return all fields" in forAll { (values: List[Json]) =>
    val fields = values.zipWithIndex.map {
      case (value, i) => i.toString -> value
    }.reverse

    val result1 = JsonObject.fromIterable(fields)
    val result2 = JsonObject.fromFoldable(fields)

    assert(result1.toIterable.toList === fields)
    assert(result2.toIterable.toList === fields)
  }

  "toList" should "return all fields" in forAll { (values: List[Json]) =>
    val fields = values.zipWithIndex.map {
      case (value, i) => i.toString -> value
    }.reverse

    val result1 = JsonObject.fromIterable(fields)
    val result2 = JsonObject.fromFoldable(fields)

    assert(result1.toList === fields)
    assert(result2.toList === fields)
  }

  "toVector" should "return all fields" in forAll { (values: Vector[Json]) =>
    val fields = values.zipWithIndex.map {
      case (value, i) => i.toString -> value
    }.reverse

    val result1 = JsonObject.fromIterable(fields)
    val result2 = JsonObject.fromFoldable(fields)

    assert(result1.toVector === fields)
    assert(result2.toVector === fields)
  }

  "add" should "replace existing fields with the same key" in {
    forAll { (head: Json, tail: List[Json], replacement: Json) =>
      val fields = (head :: tail).zipWithIndex.map {
        case (value, i) => i.toString -> value
      }

      val result1 = JsonObject.fromIterable(fields).add("0", replacement)
      val result2 = JsonObject.fromFoldable(fields).add("0", replacement)
      val expected = JsonObject.fromFoldable(("0" -> replacement) :: fields.tail)

      assert(result1 === expected)
      assert(result2 === expected)
    }
  }

  it should "replace existing fields with the same key in the correct position" in {
    forAll { (head: Json, tail: List[Json], replacement: Json) =>
      val fields = (head :: tail).zipWithIndex.map {
        case (value, i) => i.toString -> value
      }.reverse

      val result1 = JsonObject.fromIterable(fields).add("0", replacement)
      val result2 = JsonObject.fromFoldable(fields).add("0", replacement)
      val expected = JsonObject.fromFoldable(fields.init :+ ("0" -> replacement))

      assert(result1 === expected)
      assert(result2 === expected)
    }
  }

  "add, +:, and remove" should "be applied correctly" in {
    forAll { (original: JsonObject, operations: List[Either[String, (String, Json, Boolean)]]) =>
      val result = operations.foldLeft(original) {
        case (acc, Right((key, value, true)))  => acc.add(key, value)
        case (acc, Right((key, value, false))) => (key, value) +: acc
        case (acc, Left(key))                  => acc.remove(key)
      }

      val expected = operations.foldLeft(original.toList) {
        case (acc, Right((key, value, true))) =>
          val index = acc.indexWhere(_._1 == key)

          if (index < 0) {
            acc :+ (key -> value)
          } else {
            acc.updated(index, (key, value))
          }
        case (acc, Right((key, value, false))) =>
          val index = acc.indexWhere(_._1 == key)

          if (index < 0) {
            (key -> value) :: acc
          } else {
            acc.updated(index, (key, value))
          }
        case (acc, Left(key)) => acc.filterNot(_._1 == key)
      }

      assert(result.toList === expected)
    }
  }

  "+:" should "replace existing fields with the same key" in {
    forAll { (head: Json, tail: List[Json], replacement: Json) =>
      val fields = (head :: tail).zipWithIndex.map {
        case (value, i) => i.toString -> value
      }.reverse

      val result1 = ("0" -> replacement) +: JsonObject.fromIterable(fields)
      val result2 = ("0" -> replacement) +: JsonObject.fromFoldable(fields)
      val expected = JsonObject.fromFoldable(("0" -> replacement) :: fields.init)

      assert(result1 === expected)
      assert(result2 === expected)
    }
  }

  it should "replace existing fields with the same key in the correct position" in {
    forAll { (head: Json, tail: List[Json], replacement: Json) =>
      val fields = (head :: tail).zipWithIndex.map {
        case (value, i) => i.toString -> value
      }

      val result1 = ("0" -> replacement) +: JsonObject.fromIterable(fields)
      val result2 = ("0" -> replacement) +: JsonObject.fromFoldable(fields)
      val expected = JsonObject.fromFoldable(("0" -> replacement) :: fields.tail)

      assert(result1 === expected)
      assert(result2 === expected)
    }
  }

  "mapValues" should "transform the JSON object appropriately" in forAll { (values: List[Json], replacement: Json) =>
    val fields = values.zipWithIndex.map {
      case (value, i) => i.toString -> value
    }

    val result1 = JsonObject.fromIterable(fields).mapValues(_ => replacement)
    val result2 = JsonObject.fromFoldable(fields).mapValues(_ => replacement)
    val expected = JsonObject.fromFoldable(fields.map(field => field._1 -> replacement))

    assert(result1 === expected)
    assert(result2 === expected)
  }

  "traverse" should "transform the JSON object appropriately" in forAll { (values: List[Json]) =>
    val fields = values.zipWithIndex.map {
      case (value, i) => i.toString -> value
    }

    val result1: Option[JsonObject] = JsonObject.fromIterable(fields).traverse[Option](Some(_))
    val result2: Option[JsonObject] = JsonObject.fromFoldable(fields).traverse[Option](Some(_))
    val expected = JsonObject.fromFoldable(fields)

    assert(result1 === Some(expected))
    assert(result2 === Some(expected))
  }

  it should "return values in order" in forAll { (value: JsonObject) =>
    val result = value.traverse[({ type L[x] = Const[List[Json], x] })#L](a => Const(List(a))).getConst

    assert(result === value.values.toList)
  }

  "filter" should "be consistent with Map#filter" in forAll { (value: JsonObject, pred: ((String, Json)) => Boolean) =>
    assert(value.filter(pred).toMap === value.toMap.filter(pred))
  }

  "filterKeys" should "be consistent with Map#filterKeys" in forAll { (value: JsonObject, pred: String => Boolean) =>
    assert(value.filterKeys(pred).toMap === value.toMap.filterKeys(pred).toMap)
  }

  "Eq[JsonObject]" should "be consistent with comparing fields" in {
    forAll { (fields1: List[(String, Json)], fields2: List[(String, Json)]) =>
      val result = Eq[JsonObject].eqv(JsonObject.fromIterable(fields1), JsonObject.fromIterable(fields2))
      val expected = fields1 == fields2

      assert(result === expected)
    }
  }

  "deepMerge" should "merge correctly" in forAll { (left: JsonObject, right: JsonObject) =>
    val merged = left.deepMerge(right)

    assert(merged.keys.toSet === (left.keys.toSet ++ right.keys.toSet))
    merged.toList.foreach {
      case (key, value) =>
        (left(key), right(key)) match {
          case (Some(leftVal), Some(rightVal)) => assert(value === leftVal.deepMerge(rightVal))
          case (Some(leftVal), None)           => assert(value === leftVal)
          case (None, Some(rightVal))          => assert(value === rightVal)
          case _                               => throw new TestFailedException("Impossible state reached in deepMerge test", 0)
        }
    }
  }

  it should "preserve argument order" in forAll { (js: List[Json]) =>
    val fields = js.zipWithIndex.map {
      case (j, i) => i.toString -> j
    }

    val reversed = JsonObject.fromIterable(fields.reverse)
    val merged = JsonObject.fromIterable(fields).deepMerge(reversed)

    assert(merged.toList === fields.reverse)
  }
}
