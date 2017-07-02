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

  "decodeManyBigDecimals" should "return the correct result" in {
    assert(benchmark.decodeManyBigDecimals === Right(List.fill(benchmark.count)(expectedBigDecimal)))
  }

  "decodeManyBigInts" should "return the correct result" in {
    assert(benchmark.decodeManyBigInts === Right(List.fill(benchmark.count)(expectedBigInt)))
  }

  "decodeManyDoubles" should "return the correct result" in {
    assert(benchmark.decodeManyDoubles === Right(List.fill(benchmark.count)(expectedDouble)))
  }

  "decodeManyLongs" should "return the correct result" in {
    assert(benchmark.decodeManyLongs === Right(List.fill(benchmark.count)(expectedLong)))
  }

  "parseManyBiggerDecimals" should "return the correct result" in {
    val expected = Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe(benchmark.inputBiggerDecimal))

    assert(benchmark.parseManyBiggerDecimals === Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }

  "parseManyBigDecimals" should "return the correct result" in {
    val expected = Json.fromBigDecimal(expectedBigDecimal)

    assert(benchmark.parseManyBigDecimals === Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }

  "parseManyBigInts" should "return the correct result" in {
    val expected = Json.fromBigInt(expectedBigInt)

    assert(benchmark.parseManyBigInts === Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }

  "parseManyDoubles" should "return the correct result" in {
    val expected = Json.fromDouble(expectedDouble).get

    assert(benchmark.parseManyDoubles === Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }

  "parseManyLongs" should "return the correct result" in {
    val expected = Json.fromLong(expectedLong)

    assert(benchmark.parseManyLongs === Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }
}
