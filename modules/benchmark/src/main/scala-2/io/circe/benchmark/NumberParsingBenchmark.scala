package io.circe.benchmark

import io.circe.{ Error, Json }
import io.circe.jawn
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

/**
 * Compare the performance of JSON number parsing.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.NumberParsingBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class NumberParsingBenchmark {
  val inputBiggerDecimal = "9223372036854775808e9223372036854775808"
  val inputBigDecimal = "9223372036854775808e-2147483647"
  val inputBigInt = "9223372036854775808"
  val inputDouble = "12345.6789"
  val inputLong = "123456789"

  val count = 1000

  val inputBiggerDecimals = "[" + List.fill(count)(inputBiggerDecimal).mkString(", ") + "]"
  val inputBigDecimals = "[" + List.fill(count)(inputBigDecimal).mkString(", ") + "]"
  val inputBigInts = "[" + List.fill(count)(inputBigInt).mkString(", ") + "]"
  val inputDoubles = "[" + List.fill(count)(inputDouble).mkString(", ") + "]"
  val inputLongs = "[" + List.fill(count)(inputLong).mkString(", ") + "]"

  @Benchmark
  def decodeBigDecimal: Either[Error, BigDecimal] = jawn.decode[BigDecimal](inputBigDecimal)

  @Benchmark
  def decodeBigInt: Either[Error, BigInt] = jawn.decode[BigInt](inputBigInt)

  @Benchmark
  def decodeDouble: Either[Error, Double] = jawn.decode[Double](inputDouble)

  @Benchmark
  def decodeLong: Either[Error, Long] = jawn.decode[Long](inputLong)

  @Benchmark
  def parseBiggerDecimal: Either[Error, Json] = jawn.parse(inputBiggerDecimal)

  @Benchmark
  def parseBigDecimal: Either[Error, Json] = jawn.parse(inputBigDecimal)

  @Benchmark
  def parseBigInt: Either[Error, Json] = jawn.parse(inputBigInt)

  @Benchmark
  def parseDouble: Either[Error, Json] = jawn.parse(inputDouble)

  @Benchmark
  def parseLong: Either[Error, Json] = jawn.parse(inputLong)

  @Benchmark
  def decodeManyBigDecimals: Either[Error, List[BigDecimal]] = jawn.decode[List[BigDecimal]](inputBigDecimals)

  @Benchmark
  def decodeManyBigInts: Either[Error, List[BigInt]] = jawn.decode[List[BigInt]](inputBigInts)

  @Benchmark
  def decodeManyDoubles: Either[Error, List[Double]] = jawn.decode[List[Double]](inputDoubles)

  @Benchmark
  def decodeManyLongs: Either[Error, List[Long]] = jawn.decode[List[Long]](inputLongs)

  @Benchmark
  def parseManyBiggerDecimals: Either[Error, Json] = jawn.parse(inputBiggerDecimals)

  @Benchmark
  def parseManyBigDecimals: Either[Error, Json] = jawn.parse(inputBigDecimals)

  @Benchmark
  def parseManyBigInts: Either[Error, Json] = jawn.parse(inputBigInts)

  @Benchmark
  def parseManyDoubles: Either[Error, Json] = jawn.parse(inputDoubles)

  @Benchmark
  def parseManyLongs: Either[Error, Json] = jawn.parse(inputLongs)
}
