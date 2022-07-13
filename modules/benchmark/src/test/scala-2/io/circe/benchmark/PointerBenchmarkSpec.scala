package io.circe.benchmark

import io.circe.Json
import munit.FunSuite

class PointerBenchmarkSpec extends FunSuite {
  val benchmark: PointerBenchmark = new PointerBenchmark

  test("goodOptics should succeed correctly") {
    assertEquals(benchmark.goodOptics, Some(Json.fromInt(123)))
  }

  test("goodPointer should succeed correctly") {
    assertEquals(benchmark.goodPointer, Some(Json.fromInt(123)))
  }

  test("badOptics should fail") {
    assertEquals(benchmark.badOptics, None)
  }

  test("badPointer should fail") {
    assertEquals(benchmark.badPointer, None)
  }
}
