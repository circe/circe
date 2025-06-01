/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
    new Json.Folder[Int] with ((Int, Json) => Int) {
      def apply(i: Int, j: Json): Int = i + j.foldWith(this)

      def onNull: Int = 0
      def onBoolean(value: Boolean): Int = if (value) 1 else 0
      def onNumber(value: JsonNumber): Int = value.toDouble.toInt
      def onString(value: String): Int = value.length
      def onArray(value: Vector[Json]): Int = value.foldLeft(0)(this)
      def onObject(value: JsonObject): Int = value.values.foldLeft(0)(this)
    }
  )

  @Benchmark
  def withFold: Int = {
    def foldToInt(json: Json): Int = json.fold(
      0,
      if (_) 1 else 0,
      _.toDouble.toInt,
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
      case Json.JNull           => 0
      case Json.JBoolean(value) => if (value) 1 else 0
      case Json.JNumber(value)  => value.toDouble.toInt
      case Json.JString(value)  => value.length
      case Json.JArray(value)   =>
        value.foldLeft(0) {
          case (acc, json) => acc + foldToInt(json)
        }
      case Json.JObject(value) =>
        value.values.foldLeft(0) {
          case (acc, json) => acc + foldToInt(json)
        }
    }

    foldToInt(doc)
  }
}
