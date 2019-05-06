package io.circe.benchmark

import io.circe.{ Json, JsonObject }
import org.scalatest.flatspec.AnyFlatSpec

class JsonObjectBenchmarkSpec extends AnyFlatSpec {
  val benchmark: JsonObjectBenchmark = new JsonObjectBenchmark

  "buildWithFromIterable" should "build the correct JsonObject" in {
    assert(benchmark.buildWithFromIterable === benchmark.valueFromIterable)
  }

  "buildWithFromFoldable" should "build the correct JsonObject" in {
    assert(benchmark.buildWithFromFoldable === benchmark.valueFromIterable)
  }

  "buildWithAdd" should "build the correct JsonObject" in {
    assert(benchmark.buildWithAdd === benchmark.valueFromIterable)
  }

  "lookupGoodFromIterable" should "return the correct result" in {
    assert(benchmark.lookupGoodFromIterable === Some(Json.fromInt(50)))
  }

  "lookupBadFromIterable" should "return the correct result" in {
    assert(benchmark.lookupBadFromIterable === None)
  }

  "lookupGoodFromFoldable" should "return the correct result" in {
    assert(benchmark.lookupGoodFromFoldable === Some(Json.fromInt(50)))
  }

  "lookupBadFromFoldable" should "return the correct result" in {
    assert(benchmark.lookupBadFromFoldable === None)
  }

  "remove" should "return the correct result" in {
    val expected = benchmark.fields.flatMap {
      case ("0", _)     => None
      case ("50", _)    => None
      case ("51", _)    => None
      case ("99", _)    => None
      case (key, value) => Some((key, value))
    }

    assert(benchmark.remove === JsonObject.fromIterable(expected))
  }
}
