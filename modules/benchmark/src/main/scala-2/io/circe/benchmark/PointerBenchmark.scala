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

import io.circe.Json
import io.circe.optics.JsonPath
import io.circe.pointer.Pointer
import io.circe.syntax._
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

/**
 * Compare the performance of various ways of folding JSON values.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.PointerBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class PointerBenchmark {
  val good: Json = Json.obj(
    "foo" := List("foo".asJson, true.asJson, Json.obj("bar" := Json.obj("baz" := Json.obj("qux" := 123.asJson))))
  )
  val bad: Json = Json.obj(
    "foo" := List("foo".asJson, true.asJson, Json.obj("bar" := Json.obj("baz" := Json.obj("quux" := 123.asJson))))
  )

  val path = JsonPath.root.foo(2).bar.baz.qux.json
  val Right(pointer) = Pointer.parse("/foo/2/bar/baz/qux"): @unchecked

  @Benchmark
  def goodOptics: Option[Json] = path.getOption(good)

  @Benchmark
  def goodPointer: Option[Json] = pointer.getOption(good)

  @Benchmark
  def badOptics: Option[Json] = path.getOption(bad)

  @Benchmark
  def badPointer: Option[Json] = pointer.getOption(bad)
}
