package io.circe

import io.circe.tests.CirceSuite

class JsonSuite extends CirceSuite {

  test("deepmerge preserves argument order") {
    check { (js: List[Json]) =>
      val fields = js.zipWithIndex.map {
        case (j, i) => i.toString -> j
      }

      val reversed = Json.fromFields(fields.reverse)
      val merged   = Json.fromFields(fields).deepMerge(reversed)

      merged.asObject.map { _.toList } === Some(fields.reverse)
    }
  }

}
