package io.circe.benchmark

import io.circe.{ Json, JsonNumber, JsonObject }
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

/**
 * Compare the performance of various ways of folding JSON values.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.FoldingBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class FoldingBenchmark extends ExampleData {
  val doc: Json = Json.arr(intsJson, booleansJson, foosJson, helloWorldJson)

  @Benchmark
  def withFoldWith: Int = doc.foldWith(
    new Json.Folder[Int] {
      private[this] val accumulate: (Int, Json) => Int = _ + _.foldWith(this)

      def onNull: Int = 0
      def onBoolean(value: Boolean): Int = if (value) 1 else 0
      def onNumber(value: JsonNumber): Int = value.truncateToInt
      def onString(value: String): Int = value.length
      def onArray(value: Vector[Json]): Int = value.foldLeft(0)(accumulate)
      def onObject(value: JsonObject): Int = value.values.foldLeft(0)(accumulate)
    }
  )

  @Benchmark
  def withFold: Int = {
    def foldToInt(json: Json): Int = json.fold(
      0,
      if (_) 1 else 0,
      _.truncateToInt,
      _.length,
      _.foldLeft(0) {
        case (acc, json) => acc + foldToInt(json)
      },
      _.values.foldLeft(0) {
        case (acc, json) => acc + foldToInt(json)
      }
    )

    foldToInt(doc)
  }

  @Benchmark
  def withPatternMatch: Int = {
    def foldToInt(json: Json): Int = json match {
      case Json.JNull => 0
      case Json.JBoolean(value) => if (value) 1 else 0
      case Json.JNumber(value) => value.truncateToInt
      case Json.JString(value) => value.length
      case Json.JArray(value) => value.foldLeft(0) {
        case (acc, json) => acc + foldToInt(json)
      }
      case Json.JObject(value) => value.values.foldLeft(0) {
        case (acc, json) => acc + foldToInt(json)
      }
    }

    foldToInt(doc)
  }
}
