package io.circe

import cats.data.Const
import cats.instances.all._
import cats.kernel.Eq
import io.circe.tests.CirceMunitSuite
import org.scalacheck.Prop.forAll

class JsonObjectSuite extends CirceMunitSuite {
  property("JsonObject.fromIterable should drop all but the last instance when fields have the same key") {
    forAll { (key: String, values: List[Json]) =>
      val result = JsonObject.fromIterable(values.map(key -> _))
      val expected = if (values.isEmpty) JsonObject.empty else JsonObject.singleton(key, values.last)

      assertEquals(result, expected)
    }
  }

  test("JsonObject.fromIterable should maintain the first position when keys are duplicated") {
    val fields = List("a" -> Json.fromInt(0), "b" -> Json.fromInt(1), "c" -> Json.fromInt(2), "b" -> Json.fromInt(3))
    val expected = JsonObject("a" -> Json.fromInt(0), "b" -> Json.fromInt(3), "c" -> Json.fromInt(2))

    assertEquals(JsonObject.fromIterable(fields), expected)
  }

  group("JsonObject.fromFoldable should") {
    property("match JsonObject.fromIterable") {
      forAll { (fields: List[(String, Json)]) =>
        val result1 = JsonObject.fromIterable(fields)
        val result2 = JsonObject.fromFoldable(fields)

        assertEquals(result1.hashCode, result2.hashCode)
        assertEquals(result1, result2)
      }
    }

    property("drop all but the last instance when fields have the same key") {
      forAll { (key: String, values: List[Json]) =>
        val result = JsonObject.fromFoldable(values.map(key -> _))
        val expected = if (values.isEmpty) JsonObject.empty else JsonObject.singleton(key, values.last)

        assertEquals(result, expected)
      }
    }

    test("maintain the first position when keys are duplicated") {
      val fields = List("a" -> Json.fromInt(0), "b" -> Json.fromInt(1), "c" -> Json.fromInt(2), "b" -> Json.fromInt(3))
      val expected = JsonObject("a" -> Json.fromInt(0), "b" -> Json.fromInt(3), "c" -> Json.fromInt(2))

      assertEquals(JsonObject.fromFoldable(fields), expected)
    }

  }

  property("JsonObject.apply should match JsonObject.fromIterable") {
    forAll { (fields: List[(String, Json)]) =>
      assertEquals(JsonObject(fields: _*), JsonObject.fromIterable(fields))
    }
  }

  property("apply should find fields if they exist") {
    forAll { (fields: List[(String, Json)], key: String) =>
      val result1 = JsonObject.fromIterable(fields)
      val result2 = JsonObject.fromFoldable(fields)
      val expected = fields.find(_._1 == key).map(_._2)

      assertEquals(result1(key), expected)
      assertEquals(result2(key), expected)
    }
  }

  property("contains should find fields if they exist") {
    forAll { (fields: List[(String, Json)], key: String) =>
      val result1 = JsonObject.fromIterable(fields)
      val result2 = JsonObject.fromFoldable(fields)
      val expected = fields.find(_._1 == key).nonEmpty

      assertEquals(result1.contains(key), expected)
      assertEquals(result2.contains(key), expected)
    }
  }

  property("size should return the expected result") {
    forAll { (fields: List[(String, Json)]) =>
      val result1 = JsonObject.fromIterable(fields)
      val result2 = JsonObject.fromFoldable(fields)
      val expected = fields.toMap.size

      assertEquals(result1.size, expected)
      assertEquals(result2.size, expected)
    }
  }

  property("isEmpty should return the expected result") {
    forAll { (fields: List[(String, Json)]) =>
      val result1 = JsonObject.fromIterable(fields)
      val result2 = JsonObject.fromFoldable(fields)
      val expected = fields.isEmpty

      assertEquals(result1.isEmpty, expected)
      assertEquals(result2.isEmpty, expected)
    }
  }

  property("nonEmpty should return the expected result") {
    forAll { (fields: List[(String, Json)]) =>
      val result1 = JsonObject.fromIterable(fields)
      val result2 = JsonObject.fromFoldable(fields)
      val expected = fields.nonEmpty

      assertEquals(result1.nonEmpty, expected)
      assertEquals(result2.nonEmpty, expected)
    }
  }

  property("kleisli should find fields if they exist") {
    forAll { (fields: List[(String, Json)], key: String) =>
      val expected = fields.find(_._1 == key).map(_._2)

      assertEquals(JsonObject.fromIterable(fields).kleisli(key), expected)
      assertEquals(JsonObject.fromFoldable(fields).kleisli(key), expected)
    }
  }

  property("keys should return all keys") {
    forAll { (fields: List[(String, Json)]) =>
      val result1 = JsonObject.fromIterable(fields)
      val result2 = JsonObject.fromFoldable(fields)
      val expected = fields.map(_._1).distinct

      assertEquals(result1.keys.toList, expected)
      assertEquals(result2.keys.toList, expected)
    }
  }

  property("values should return all values") {
    forAll { (fields: List[(String, Json)]) =>
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

      assertEquals(result1.values.toList, expected)
      assertEquals(result2.values.toList, expected)
    }
  }

  property("toMap should round-trip through JsonObject.fromMap") {
    forAll { (fields: List[(String, Json)]) =>
      val result1 = JsonObject.fromIterable(fields)
      val result2 = JsonObject.fromFoldable(fields)
      val expected = result1

      assertEquals(JsonObject.fromMap(result1.toMap), expected)
      assertEquals(JsonObject.fromMap(result2.toMap), expected)
    }
  }

  property("toIterable should return all fields") {
    forAll { (values: List[Json]) =>
      val fields = values.zipWithIndex.map {
        case (value, i) => i.toString -> value
      }.reverse

      val result1 = JsonObject.fromIterable(fields)
      val result2 = JsonObject.fromFoldable(fields)

      assertEquals(result1.toIterable.toList, fields)
      assertEquals(result2.toIterable.toList, fields)
    }
  }

  property("toList should return all fields") {
    forAll { (values: List[Json]) =>
      val fields = values.zipWithIndex.map {
        case (value, i) => i.toString -> value
      }.reverse

      val result1 = JsonObject.fromIterable(fields)
      val result2 = JsonObject.fromFoldable(fields)

      assertEquals(result1.toList, fields)
      assertEquals(result2.toList, fields)
    }
  }

  property("toVector should return all fields") {
    forAll { (values: Vector[Json]) =>
      val fields = values.zipWithIndex.map {
        case (value, i) => i.toString -> value
      }.reverse

      val result1 = JsonObject.fromIterable(fields)
      val result2 = JsonObject.fromFoldable(fields)

      assertEquals(result1.toVector, fields)
      assertEquals(result2.toVector, fields)
    }
  }

  property("add should replace existing fields with the same key") {
    forAll { (head: Json, tail: List[Json], replacement: Json) =>
      val fields = (head :: tail).zipWithIndex.map {
        case (value, i) => i.toString -> value
      }

      val expected = JsonObject.fromFoldable(("0" -> replacement) :: fields.tail)

      assertEquals(JsonObject.fromIterable(fields).add("0", replacement), expected)
      assertEquals(JsonObject.fromFoldable(fields).add("0", replacement), expected)
    }
  }

  property("add should replace existing fields with the same key in the correct position") {
    forAll { (head: Json, tail: List[Json], replacement: Json) =>
      val fields = (head :: tail).zipWithIndex.map {
        case (value, i) => i.toString -> value
      }.reverse

      val result1 = JsonObject.fromIterable(fields).add("0", replacement)
      val result2 = JsonObject.fromFoldable(fields).add("0", replacement)
      val expected = JsonObject.fromFoldable(fields.init :+ ("0" -> replacement))

      assertEquals(result1, expected)
      assertEquals(result2, expected)
    }
  }

  property("add, +:, and remove should be applied correctly") {
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

      assertEquals(result.toList, expected)
    }
  }

  property("+: should replace existing fields with the same key") {
    forAll { (head: Json, tail: List[Json], replacement: Json) =>
      val fields = (head :: tail).zipWithIndex.map {
        case (value, i) => i.toString -> value
      }.reverse

      val result1 = ("0" -> replacement) +: JsonObject.fromIterable(fields)
      val result2 = ("0" -> replacement) +: JsonObject.fromFoldable(fields)
      val expected = JsonObject.fromFoldable(("0" -> replacement) :: fields.init)

      assertEquals(result1, expected)
      assertEquals(result2, expected)
    }
  }

  property("+: should replace existing fields with the same key in the correct position") {
    forAll { (head: Json, tail: List[Json], replacement: Json) =>
      val fields = (head :: tail).zipWithIndex.map {
        case (value, i) => i.toString -> value
      }

      val result1 = ("0" -> replacement) +: JsonObject.fromIterable(fields)
      val result2 = ("0" -> replacement) +: JsonObject.fromFoldable(fields)
      val expected = JsonObject.fromFoldable(("0" -> replacement) :: fields.tail)

      assertEquals(result1, expected)
      assertEquals(result2, expected)
    }
  }

  property("mapValues should transform the JSON object appropriately") {
    forAll { (values: List[Json], replacement: Json) =>
      val fields = values.zipWithIndex.map {
        case (value, i) => i.toString -> value
      }

      val expected = JsonObject.fromFoldable(fields.map(field => field._1 -> replacement))
      assertEquals(JsonObject.fromIterable(fields).mapValues(_ => replacement), expected)
      assertEquals(JsonObject.fromFoldable(fields).mapValues(_ => replacement), expected)
    }
  }

  property("traverse should transform the JSON object appropriately") {
    forAll { (values: List[Json]) =>
      val fields = values.zipWithIndex.map {
        case (value, i) => i.toString -> value
      }

      val result1: Option[JsonObject] = JsonObject.fromIterable(fields).traverse[Option](Some(_))
      val result2: Option[JsonObject] = JsonObject.fromFoldable(fields).traverse[Option](Some(_))
      val expected = JsonObject.fromFoldable(fields)

      assertEquals(result1, Some(expected))
      assertEquals(result2, Some(expected))
    }
  }

  property("traverse  should return values in order") {
    forAll { (value: JsonObject) =>
      val result = value.traverse[({ type L[x] = Const[List[Json], x] })#L](a => Const(List(a))).getConst

      assertEquals(result, value.values.toList)
    }
  }

  property("filter should be consistent with Map#filter") {
    forAll { (value: JsonObject, pred: ((String, Json)) => Boolean) =>
      assertEquals(value.filter(pred).toMap, value.toMap.filter(pred))
    }
  }

  property("filterKeys should be consistent with Map#filterKeys") {
    forAll { (value: JsonObject, pred: String => Boolean) =>
      assertEquals(value.filterKeys(pred).toMap, value.toMap.filterKeys(pred).toMap)
    }
  }

  property("Eq[JsonObject] should be consistent with comparing fields") {
    forAll { (fields1: List[(String, Json)], fields2: List[(String, Json)]) =>
      val result = Eq[JsonObject].eqv(JsonObject.fromIterable(fields1), JsonObject.fromIterable(fields2))
      assertEquals(result, fields1 == fields2)
    }
  }

  property("deepMerge should merge correctly") {
    forAll { (left: JsonObject, right: JsonObject) =>
      val merged = left.deepMerge(right)

      assertEquals(merged.keys.toSet, left.keys.toSet ++ right.keys.toSet)
      merged.toList.foreach {
        case (key, value) =>
          (left(key), right(key)) match {
            case (Some(leftVal), Some(rightVal)) => assertEquals(value, leftVal.deepMerge(rightVal))
            case (Some(leftVal), None)           => assertEquals(value, leftVal)
            case (None, Some(rightVal))          => assertEquals(value, rightVal)
            case _                               => throw new Exception("Impossible state reached in deepMerge test")
          }
      }
    }
  }

  property("deepMerge should preserve argument order") {
    forAll { (js: List[Json]) =>
      val fields = js.zipWithIndex.map {
        case (j, i) => i.toString -> j
      }

      val reversed = JsonObject.fromIterable(fields.reverse)
      val merged = JsonObject.fromIterable(fields).deepMerge(reversed)

      assertEquals(merged.toList, fields.reverse)
    }
  }

}
