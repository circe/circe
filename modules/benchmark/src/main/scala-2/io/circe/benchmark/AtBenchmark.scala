package io.circe.benchmark

import io.circe.{ Decoder, Json }
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

/**
 * Compare the performance of various ways of folding JSON values.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.AtBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class AtBenchmark extends ExampleData {
  val item: Json = Json.obj("a" -> Json.fromString("foo"), "b" -> Json.fromInt(101))
  val doc: Json = Json.fromValues(List.fill(256)(item))

  @Benchmark
  def at: Decoder.Result[List[(String, Int)]] = {
    val decoder = for {
      a <- Decoder[String].at("a")
      b <- Decoder[Int].at("b")
    } yield (a, b)

    Decoder.decodeList(decoder).decodeJson(doc)
  }
}
