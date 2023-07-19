/*
 * Copyright 2023 circe
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

import io.circe.{ Decoder, Encoder }
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

/**
 * Compare the performance of derived and non-derived codecs.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.MapDecoderBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class MapDecoderBenchmark {
  val data = (0 to 100).map(i => "a" * i -> (0 to i).map(j => "b" * j -> j).toMap).toMap

  val json = Encoder[Map[String, Map[String, Int]]].apply(data)

  @Benchmark
  def decodeNestedMap: Decoder.Result[Map[String, Map[String, Int]]] =
    Decoder[Map[String, Map[String, Int]]].decodeJson(json)
}
