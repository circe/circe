package io.circe

import io.circe.tests.CirceSuite
import cats.data.Const

class JsonObjectSuite extends CirceSuite {
  "apply" should "match fromIterable" in { (fields: Vector[(String, Json)]) =>
    assert(JsonObject(fields: _*) === JsonObject.fromIterable(fields))
  }

  "from" should "match fromIterable" in { (fields: Vector[(String, Json)]) =>
    assert(JsonObject.from(fields) === JsonObject.fromIterable(fields))
  }

  "+:" should "replace existing fields with the same key" in forAll { (j: Json, h: Json, t: List[Json]) =>
    val fields = (h :: t).zipWithIndex.map {
      case (j, i) => i.toString -> j
    }

    assert((("0" -> j) +: JsonObject.from(fields)).toList === (("0" -> j) :: fields.tail))
  }

  "size" should "return the size of the JSON object" in forAll { (js: List[Json]) =>
    val fields = js.zipWithIndex.map {
      case (j, i) => i.toString -> j
    }

    assert(JsonObject.from(fields).size === fields.size)
  }

  "withJsons" should "transform the JSON object appropriately" in forAll { (j: Json, js: List[Json]) =>
    val fields = js.zipWithIndex.map {
      case (j, i) => i.toString -> j
    }

    assert(JsonObject.from(fields).withJsons(_ => j).values === Vector.fill(js.size)(j))
  }

  "toList" should "return the appropriate list of key-value pairs" in forAll { (js: List[Json]) =>
    val fields = js.zipWithIndex.map {
      case (j, i) => i.toString -> j
    }.reverse

    assert(JsonObject.from(fields).toList === fields)
  }

  "values" should "return the values in the JSON object" in forAll { (js: Vector[Json]) =>
    val fields = js.zipWithIndex.map {
      case (j, i) => i.toString -> j
    }.reverse

    assert(JsonObject.from(fields).values === fields.map(_._2))
  }

  "traverse" should "transform the JSON object appropriately" in forAll { (js: List[Json]) =>
    val fields = js.zipWithIndex.map {
      case (j, i) => i.toString -> j
    }

    val o = JsonObject.from(fields)

    assert(o.traverse[Option](j => Some(j)) === Some(o))
  }

  it should "return values in order" in forAll { json: JsonObject =>
    assert(json.traverse[({ type L[x] = Const[Vector[Json], x] })#L](a => Const(Vector(a))).getConst === json.values)
  }

  "filter" should "be consistent with Map#filter" in forAll { (obj: JsonObject, pred: ((String, Json)) => Boolean) =>
    assert(obj.filter(pred).toMap === obj.toMap.filter(pred))
  }

  "filterKeys" should "be consistent with Map#filterKeys" in forAll { (obj: JsonObject, pred: String => Boolean) =>
    assert(obj.filterKeys(pred).toMap === obj.toMap.filterKeys(pred))
  }
}
