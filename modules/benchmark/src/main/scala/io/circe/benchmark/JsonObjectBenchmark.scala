package io.circe.benchmark

import cats.instances.list._
import io.circe.{ Json, JsonObject }
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

/**
 * Compare the performance of JSON object operations.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.JsonObjectBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class JsonObjectBenchmark {
  val count = 100
  val fields: List[(String, Json)] = (1 to count).map(i => (i.toString, Json.fromInt(i))).toList
  val value: JsonObject = JsonObject.fromIterable(fields)

  @Benchmark
  def buildWithFromIterable: JsonObject = JsonObject.fromIterable(fields)

  @Benchmark
  def buildWithFrom: JsonObject = JsonObject.from(fields)

  @Benchmark
  def buildWithAdd: JsonObject = fields.foldLeft(JsonObject.empty) {
    case (acc, (key, value)) => acc.add(key, value)
  }

  @Benchmark
  def lookupGood: Option[Json] = value("50")

  @Benchmark
  def lookupBad: Option[Json] = value("abc")

  @Benchmark
  def remove: JsonObject = value.remove("50").remove("51").remove("abc").remove("99").remove("0")
}
