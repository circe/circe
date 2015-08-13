package io.circe

import cats.laws.discipline.eq._
import io.circe.Cats._
import io.circe.test.CirceSuite
import org.scalacheck.{ Arbitrary, Gen }

class JsonObjectTests extends CirceSuite {
  test("+: with duplicate") {
    check { (j: Json, h: Json, t: List[Json]) =>
      val fields = (h :: t).zipWithIndex.map {
        case (j, i) => i.toString -> j
      }

      (("0" -> j) +: JsonObject.from(fields)).toList === (("0" -> j) :: fields.tail)
    }
  }

  test("size") {
    check { (js: List[Json]) =>
      val fields = js.zipWithIndex.map {
        case (j, i) => i.toString -> j
      }

      JsonObject.from(fields).size === fields.size
    }
  }

  test("withJsons") {
    check { (j: Json, js: List[Json]) =>
      val fields = js.zipWithIndex.map {
        case (j, i) => i.toString -> j
      }

      JsonObject.from(fields).withJsons(_ => j).values === List.fill(js.size)(j)
    }
  }

  test("toList") {
    check { (js: List[Json]) =>
      val fields = js.zipWithIndex.map {
        case (j, i) => i.toString -> j
      }.reverse

      JsonObject.from(fields).toList === fields
    }
  }

  test("values") {
    check { (js: List[Json]) =>
      val fields = js.zipWithIndex.map {
        case (j, i) => i.toString -> j
      }.reverse

      JsonObject.from(fields).values === fields.map(_._2)
    }
  }

  test("traverse") {
    check { (js: List[Json]) =>
      val fields = js.zipWithIndex.map {
        case (j, i) => i.toString -> j
      }

      val o = JsonObject.from(fields)

      o.traverse[Option](j => Some(j)) === Some(o)
    }
  }
}