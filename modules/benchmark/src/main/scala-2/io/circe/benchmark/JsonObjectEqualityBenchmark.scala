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

import io.circe.{ Json, JsonObject }
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*
import java.util

/**
 * Benchmark equality- and hash-based operations on JSON objects
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.JsonObjectEqualityBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(value = 1, jvmArgsAppend = Array("-Xms2G", "-Xmx2G", "-XX:+AlwaysPreTouch", "-XX:+UseSerialGC"))
class JsonObjectEqualityBenchmark {

  val count: Int = 100
  val mapAndVectorObject1: JsonObject = buildMapAndVectorObject()
  val mapAndVectorObject2: JsonObject = buildMapAndVectorObject()
  val linkedHashMapObject1: JsonObject = buildLinkedHashMapObject()
  val linkedHashMapObject2: JsonObject = buildLinkedHashMapObject()

  private def buildMapAndVectorObject(): JsonObject = {
    val orderedFields: Vector[(String, Json)] =
      Vector.tabulate(count)(i => (i.toString, Json.fromInt(i)))
    val vector: Vector[String] = orderedFields.map(_._1)
    val map: Map[String, Json] = orderedFields.toMap
    JsonObject.fromMapAndVector(map, vector)
  }

  private def buildLinkedHashMapObject(): JsonObject = {
    val map = new util.LinkedHashMap[String, Json]()
    Iterator.tabulate(count)(i => (i.toString, Json.fromInt(i))).foreach { case (key, value) => map.put(key, value) }
    JsonObject.fromLinkedHashMap(map)
  }

  @Benchmark
  def equalsMapAndVector: Boolean = mapAndVectorObject1 == mapAndVectorObject2

  @Benchmark
  def hashCodeMapAndVector: Int = mapAndVectorObject1.hashCode

  @Benchmark
  def equalsLinkedHashMap: Boolean = linkedHashMapObject1 == linkedHashMapObject2

  @Benchmark
  def hashCodeLinkedHashMap: Int = linkedHashMapObject1.hashCode

  @Benchmark
  def equalsMixed: Boolean = linkedHashMapObject1 == mapAndVectorObject1
}
