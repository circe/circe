package io.circe.jackson

import cats.data.Xor
import io.circe.Json
import io.circe.tests.CirceSuite

class JacksonPrintingSuite extends CirceSuite {
  test("Printing should result in round-trippable output") {
    check { (json: Json) =>
      io.circe.jawn.parse(jacksonPrint(json)) === Xor.right(json)
    }
  }
}
