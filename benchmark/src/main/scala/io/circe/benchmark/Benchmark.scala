package io.circe.benchmark

import argonaut.{ Json => JsonA, _ }, Argonaut._
import io.circe.{ Json => JsonC, Encoder }
import io.circe.generic.auto._
import io.circe.jawn._
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

case class Foo(s: String, d: Double, i: Int, l: Long, bs: List[Boolean])

object Foo {
  implicit val codecFoo: CodecJson[Foo] = CodecJson.derive[Foo]
}

class ExampleData {
  val ints: List[Int] = (0 to 1000).toList

  val foos: Map[String, Foo] = List.tabulate(100) { i =>
    ("b" * i) -> Foo("a" * i, i + 1.0 / i, i, i * 1000L, (0 to i).map(_ % 2 == 0).toList)
  }.toMap

  @inline def encodeA[A](a: A)(implicit encode: EncodeJson[A]): JsonA = encode(a)
  @inline def encodeC[A](a: A)(implicit encode: Encoder[A]): JsonC = encode(a)

  val intsC: JsonC = encodeC(ints)
  val intsA: JsonA = encodeA(ints)

  val foosC: JsonC = encodeC(foos)
  val foosA: JsonA = encodeA(foos)

  val intsJson: String = intsC.noSpaces
  val foosJson: String = foosC.noSpaces
}

/**
 * Compare the performance of encoding operations.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.EncodingBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class EncodingBenchmark extends ExampleData {

  @Benchmark
  def encodeIntsC: JsonC = encodeC(ints)

  @Benchmark
  def encodeIntsA: JsonA = encodeA(ints)

  @Benchmark
  def encodeFoosC: JsonC = encodeC(foos)

  @Benchmark
  def encodeFoosA: JsonA = encodeA(foos)
}

/**
 * Compare the performance of decoding operations.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.DecodingBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class DecodingBenchmark extends ExampleData {
  @Benchmark
  def decodeIntsC: List[Int] = intsC.as[List[Int]].getOrElse(throw new Exception)

  @Benchmark
  def decodeIntsA: List[Int] = intsA.as[List[Int]].result.getOrElse(throw new Exception)

  @Benchmark
  def decodeFoosC: Map[String, Foo] =
    foosC.as[Map[String, Foo]].getOrElse(throw new Exception)

  @Benchmark
  def decodeFoosA: Map[String, Foo] =
    foosA.as[Map[String, Foo]].result.getOrElse(throw new Exception)
}

/**
 * Compare the performance of parsing operations.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.ParsingBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class ParsingBenchmark extends ExampleData {
  @Benchmark
  def parseIntsC: JsonC = parse(intsJson).getOrElse(throw new Exception)

  @Benchmark
  def parseIntsA: JsonA = Parse.parse(intsJson).getOrElse(throw new Exception)

  @Benchmark
  def parseFoosC: JsonC = parse(foosJson).getOrElse(throw new Exception)

  @Benchmark
  def parseFoosA: JsonA = Parse.parse(foosJson).getOrElse(throw new Exception)
}

/**
 * Compare the performance of printing operations.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.PrintingBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class PrintingBenchmark extends ExampleData {
  @Benchmark
  def printIntsC: String = intsC.noSpaces

  @Benchmark
  def printIntsA: String = intsA.nospaces

  @Benchmark
  def printFoosC: String = foosC.noSpaces

  @Benchmark
  def printFoosA: String = foosA.nospaces
}
