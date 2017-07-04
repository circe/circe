package io.circe.benchmark

import io.circe.{ Json, JsonObject }
import org.scalatest.FlatSpec

class JsonObjectBenchmarkSpec extends FlatSpec {
  val benchmark: JsonObjectBenchmark = new JsonObjectBenchmark

  "buildWithFromIterable" should "build the correct JsonObject" in {
    assert(benchmark.buildWithFromIterable === benchmark.value)
  }

  "buildWithFrom" should "build the correct JsonObject" in {
    assert(benchmark.buildWithFrom === benchmark.value)
  }

  "buildWithAdd" should "build the correct JsonObject" in {
    assert(benchmark.buildWithAdd === benchmark.value)
  }

  "lookupGood" should "return the correct result" in {
    assert(benchmark.lookupGood === Some(Json.fromInt(50)))
  }

  "lookupBad" should "return the correct result" in {
    assert(benchmark.lookupBad === None)
  }

  "remove" should "return the correct result" in {
    val expected = benchmark.fields.flatMap {
      case ("0", _) => None
      case ("50", _) => None
      case ("51", _) => None
      case ("99", _) => None
      case (key, value) => Some((key, value))
    }

    assert(benchmark.remove === JsonObject.fromIterable(expected))
  }
}
