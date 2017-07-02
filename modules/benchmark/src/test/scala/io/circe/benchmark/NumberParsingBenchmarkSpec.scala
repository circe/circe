package io.circe.benchmark

import io.circe.{ Json, JsonNumber }
import org.scalatest.FlatSpec

class NumberParsingBenchmarkSpec extends FlatSpec {
  val benchmark: NumberParsingBenchmark = new NumberParsingBenchmark

  val expectedBigDecimal = BigDecimal(benchmark.inputBigDecimal)
  val expectedBigInt = BigInt(benchmark.inputBigInt)
  val expectedDouble = benchmark.inputDouble.toDouble
  val expectedLong = benchmark.inputLong.toLong

  "decodeBigDecimal" should "return the correct result" in {
    assert(benchmark.decodeBigDecimal === Right(expectedBigDecimal))
  }

  "decodeBigInt" should "return the correct result" in {
    assert(benchmark.decodeBigInt === Right(expectedBigInt))
  }

  "decodeDouble" should "return the correct result" in {
    assert(benchmark.decodeDouble === Right(expectedDouble))
  }

  "decodeLong" should "return the correct result" in {
    assert(benchmark.decodeLong === Right(expectedLong))
  }

  "parseBiggerDecimal" should "return the correct result" in {
    val expected = Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe(benchmark.inputBiggerDecimal))

    assert(benchmark.parseBiggerDecimal === Right(expected))
  }

  "parseBigDecimal" should "return the correct result" in {
    assert(benchmark.parseBigDecimal === Right(Json.fromBigDecimal(expectedBigDecimal)))
  }

  "parseBigInt" should "return the correct result" in {
    assert(benchmark.parseBigInt === Right(Json.fromBigInt(expectedBigInt)))
  }

  "parseDouble" should "return the correct result" in {
    assert(benchmark.parseDouble === Right(Json.fromDouble(expectedDouble).get))
  }

  "parseLong" should "return the correct result" in {
    assert(benchmark.parseLong === Right(Json.fromLong(expectedLong)))
  }

  "decodeBigDecimals" should "return the correct result" in {
    assert(benchmark.decodeBigDecimals === Right(List.fill(benchmark.count)(expectedBigDecimal)))
  }

  "decodeBigInts" should "return the correct result" in {
    assert(benchmark.decodeBigInts === Right(List.fill(benchmark.count)(expectedBigInt)))
  }

  "decodeDoubles" should "return the correct result" in {
    assert(benchmark.decodeDoubles === Right(List.fill(benchmark.count)(expectedDouble)))
  }

  "decodeLongs" should "return the correct result" in {
    assert(benchmark.decodeLongs === Right(List.fill(benchmark.count)(expectedLong)))
  }

  "parseBiggerDecimals" should "return the correct result" in {
    val expected = Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe(benchmark.inputBiggerDecimal))

    assert(benchmark.parseBiggerDecimals === Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }

  "parseBigDecimals" should "return the correct result" in {
    val expected = Json.fromBigDecimal(expectedBigDecimal)

    assert(benchmark.parseBigDecimals === Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }

  "parseBigInts" should "return the correct result" in {
    val expected = Json.fromBigInt(expectedBigInt)

    assert(benchmark.parseBigInts === Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }

  "parseDoubles" should "return the correct result" in {
    val expected = Json.fromDouble(expectedDouble).get

    assert(benchmark.parseDoubles === Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }

  "parseLongs" should "return the correct result" in {
    val expected = Json.fromLong(expectedLong)

    assert(benchmark.parseLongs === Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }
}
