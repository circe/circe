package io.circe.jackson

import cats.data.Xor
import io.circe.Json
import io.circe.tests.CirceSuite

class JacksonPrintingSuite extends CirceSuite {
  "jacksonPrint" should "produce round-trippable output" in forAll { (json: Json) =>
    assert(io.circe.jawn.parse(jacksonPrint(json)) === Xor.right(json))
  }
}
