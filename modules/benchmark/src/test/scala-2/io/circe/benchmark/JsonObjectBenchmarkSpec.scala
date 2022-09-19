package io.circe.benchmark

import io.circe.{ Json, JsonObject }
import munit.FunSuite
import cats.syntax.eq._

class JsonObjectBenchmarkSpec extends FunSuite {
  val benchmark: JsonObjectBenchmark = new JsonObjectBenchmark

  test("buildWithFromIterable should build the correct JsonObject") {
    assertEquals(benchmark.buildWithFromIterable, benchmark.valueFromIterable)
  }

  test("buildWithFromFoldable should build the correct JsonObject") {
    assertEquals(benchmark.buildWithFromFoldable, benchmark.valueFromIterable)
  }

  test("buildWithAdd should build the correct JsonObject") {
    assertEquals(benchmark.buildWithAdd, benchmark.valueFromIterable)
  }

  test("lookupGoodFromIterable should return the correct result") {
    assertEquals(benchmark.lookupGoodFromIterable, Some(Json.fromInt(50)))
  }

  test("lookupBadFromIterable should return the correct result") {
    assertEquals(benchmark.lookupBadFromIterable, None)
  }

  test("lookupGoodFromFoldable should return the correct result") {
    assertEquals(benchmark.lookupGoodFromFoldable, Some(Json.fromInt(50)))
  }

  test("lookupBadFromFoldable should return the correct result") {
    assertEquals(benchmark.lookupBadFromFoldable, None)
  }

  test("remove should return the correct result") {
    val expected = benchmark.fields.flatMap {
      case ("0", _)     => None
      case ("50", _)    => None
      case ("51", _)    => None
      case ("99", _)    => None
      case (key, value) => Some((key, value))
    }

    assertEquals(benchmark.remove, JsonObject.fromIterable(expected))
  }
}
