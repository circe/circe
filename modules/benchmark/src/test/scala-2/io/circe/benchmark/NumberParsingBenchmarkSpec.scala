package io.circe.benchmark

import io.circe.{ Json, JsonNumber }
import munit.FunSuite
import cats.syntax.eq._

class NumberParsingBenchmarkSpec extends FunSuite {
  val benchmark: NumberParsingBenchmark = new NumberParsingBenchmark

  val expectedBigDecimal = BigDecimal(benchmark.inputBigDecimal)
  val expectedBigInt = BigInt(benchmark.inputBigInt)
  val expectedDouble = benchmark.inputDouble.toDouble
  val expectedLong = benchmark.inputLong.toLong

  test("decodeBigDecimal should return the correct result") {
    assertEquals(benchmark.decodeBigDecimal, Right(expectedBigDecimal))
  }

  test("decodeBigInt should return the correct result") {
    assertEquals(benchmark.decodeBigInt, Right(expectedBigInt))
  }

  test("decodeDouble should return the correct result") {
    assertEquals(benchmark.decodeDouble, Right(expectedDouble))
  }

  test("decodeLong should return the correct result") {
    assertEquals(benchmark.decodeLong, Right(expectedLong))
  }

  test("parseBiggerDecimal should return the correct result") {
    val expected = Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe(benchmark.inputBiggerDecimal))

    assertEquals(benchmark.parseBiggerDecimal, Right(expected))
  }

  test("parseBigDecimal should return the correct result") {
    assertEquals(benchmark.parseBigDecimal, Right(Json.fromBigDecimal(expectedBigDecimal)))
  }

  test("parseBigInt should return the correct result") {
    assertEquals(benchmark.parseBigInt, Right(Json.fromBigInt(expectedBigInt)))
  }

  test("parseDouble should return the correct result") {
    assertEquals(benchmark.parseDouble, Right(Json.fromDouble(expectedDouble).get))
  }

  test("parseLong should return the correct result") {
    assertEquals(benchmark.parseLong, Right(Json.fromLong(expectedLong)))
  }

  test("decodeManyBigDecimals should return the correct result") {
    assertEquals(benchmark.decodeManyBigDecimals, Right(List.fill(benchmark.count)(expectedBigDecimal)))
  }

  test("decodeManyBigInts should return the correct result") {
    assertEquals(benchmark.decodeManyBigInts, Right(List.fill(benchmark.count)(expectedBigInt)))
  }

  test("decodeManyDoubles should return the correct result") {
    assertEquals(benchmark.decodeManyDoubles, Right(List.fill(benchmark.count)(expectedDouble)))
  }

  test("decodeManyLongs should return the correct result") {
    assertEquals(benchmark.decodeManyLongs, Right(List.fill(benchmark.count)(expectedLong)))
  }

  test("parseManyBiggerDecimals should return the correct result") {
    val expected = Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe(benchmark.inputBiggerDecimal))

    assertEquals(benchmark.parseManyBiggerDecimals, Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }

  test("parseManyBigDecimals should return the correct result") {
    val expected = Json.fromBigDecimal(expectedBigDecimal)

    assertEquals(benchmark.parseManyBigDecimals, Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }

  test("parseManyBigInts should return the correct result") {
    val expected = Json.fromBigInt(expectedBigInt)

    assertEquals(benchmark.parseManyBigInts, Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }

  test("parseManyDoubles should return the correct result") {
    val expected = Json.fromDouble(expectedDouble).get

    assertEquals(benchmark.parseManyDoubles, Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }

  test("parseManyLongs should return the correct result") {
    val expected = Json.fromLong(expectedLong)

    assertEquals(benchmark.parseManyLongs, Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }
}
