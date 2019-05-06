package io.circe.benchmark

import io.circe.{ Json, JsonObject }
import org.scalacheck.Properties
import org.typelevel.claimant.Claim

class JsonObjectBenchmarkSuite extends Properties("JsonObjectBenchmark") {
  val benchmark: JsonObjectBenchmark = new JsonObjectBenchmark

  property("buildWithFromIterable should build the correct JsonObject") = Claim(
    benchmark.buildWithFromIterable == benchmark.valueFromIterable
  )

  property("buildWithFromFoldable should build the correct JsonObject") = Claim(
    benchmark.buildWithFromFoldable == benchmark.valueFromIterable
  )

  property("buildWithAdd should build the correct JsonObject") = Claim(
    benchmark.buildWithAdd == benchmark.valueFromIterable
  )

  property("lookupGoodFromIterable should return the correct result") = Claim(
    benchmark.lookupGoodFromIterable == Some(Json.fromInt(50))
  )

  property("lookupBadFromIterable should return the correct result") = Claim(
    benchmark.lookupBadFromIterable == None
  )

  property("lookupGoodFromFoldable should return the correct result") = Claim(
    benchmark.lookupGoodFromFoldable == Some(Json.fromInt(50))
  )

  property("lookupBadFromFoldable should return the correct result") = Claim(
    benchmark.lookupBadFromFoldable == None
  )

  property("remove should return the correct result") = {
    val expected = benchmark.fields.flatMap {
      case ("0", _)     => None
      case ("50", _)    => None
      case ("51", _)    => None
      case ("99", _)    => None
      case (key, value) => Some((key, value))
    }

    Claim(benchmark.remove == JsonObject.fromIterable(expected))
  }
}
