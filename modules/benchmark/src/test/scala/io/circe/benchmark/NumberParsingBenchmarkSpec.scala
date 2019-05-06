package io.circe.benchmark

import io.circe.{ Json, JsonNumber }
import org.scalacheck.Properties
import org.typelevel.claimant.Claim

class NumberParsingBenchmarkSuite extends Properties("NumberParsingBenchmark") {
  val benchmark: NumberParsingBenchmark = new NumberParsingBenchmark

  val expectedBigDecimal = BigDecimal(benchmark.inputBigDecimal)
  val expectedBigInt = BigInt(benchmark.inputBigInt)
  val expectedDouble = benchmark.inputDouble.toDouble
  val expectedLong = benchmark.inputLong.toLong

  property("decodeBigDecimal should return the correct result") = Claim(
    benchmark.decodeBigDecimal == Right(expectedBigDecimal)
  )

  property("decodeBigInt should return the correct result") = Claim(
    benchmark.decodeBigInt == Right(expectedBigInt)
  )

  property("decodeDouble should return the correct result") = Claim(
    benchmark.decodeDouble == Right(expectedDouble)
  )

  property("decodeLong should return the correct result") = Claim(
    benchmark.decodeLong == Right(expectedLong)
  )

  property("parseBiggerDecimal should return the correct result") = {
    val expected = Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe(benchmark.inputBiggerDecimal))

    Claim(benchmark.parseBiggerDecimal == Right(expected))
  }

  property("parseBigDecimal should return the correct result") = Claim(
    benchmark.parseBigDecimal == Right(Json.fromBigDecimal(expectedBigDecimal))
  )

  property("parseBigInt should return the correct result") = Claim(
    benchmark.parseBigInt == Right(Json.fromBigInt(expectedBigInt))
  )

  property("parseDouble should return the correct result") = Claim(
    benchmark.parseDouble == Right(Json.fromDouble(expectedDouble).get)
  )

  property("parseLong should return the correct result") = Claim(
    benchmark.parseLong == Right(Json.fromLong(expectedLong))
  )

  property("decodeManyBigDecimals should return the correct result") = Claim(
    benchmark.decodeManyBigDecimals == Right(List.fill(benchmark.count)(expectedBigDecimal))
  )

  property("decodeManyBigInts should return the correct result") = Claim(
    benchmark.decodeManyBigInts == Right(List.fill(benchmark.count)(expectedBigInt))
  )

  property("decodeManyDoubles should return the correct result") = Claim(
    benchmark.decodeManyDoubles == Right(List.fill(benchmark.count)(expectedDouble))
  )

  property("decodeManyLongs should return the correct result") = Claim(
    benchmark.decodeManyLongs == Right(List.fill(benchmark.count)(expectedLong))
  )

  property("parseManyBiggerDecimals should return the correct result") = {
    val expected = Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe(benchmark.inputBiggerDecimal))

    Claim(benchmark.parseManyBiggerDecimals == Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }

  property("parseManyBigDecimals should return the correct result") = {
    val expected = Json.fromBigDecimal(expectedBigDecimal)

    Claim(benchmark.parseManyBigDecimals == Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }

  property("parseManyBigInts should return the correct result") = {
    val expected = Json.fromBigInt(expectedBigInt)

    Claim(benchmark.parseManyBigInts == Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }

  property("parseManyDoubles should return the correct result") = {
    val expected = Json.fromDouble(expectedDouble).get

    Claim(benchmark.parseManyDoubles == Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }

  property("parseManyLongs should return the correct result") = {
    val expected = Json.fromLong(expectedLong)

    Claim(benchmark.parseManyLongs == Right(Json.fromValues(List.fill(benchmark.count)(expected))))
  }
}
