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

import io.circe.Printer
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

/**
 * Compare the performance of string and byte buffer printers.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.PrintingBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class PrintingBenchmark extends ExampleData {
  @Benchmark
  def printFoosToString: String = Printer.noSpaces.print(foosJson)

  @Benchmark
  def printFoosToByteBuffer: ByteBuffer = Printer.noSpaces.printToByteBuffer(foosJson)

  @Benchmark
  def printHelloWorldToString: String = Printer.noSpaces.print(helloWorldJson)

  @Benchmark
  def printHelloWorldToByteBuffer: ByteBuffer = Printer.noSpaces.printToByteBuffer(helloWorldJson)

  @Benchmark
  def printBooleansToString: String = Printer.noSpaces.print(booleansJson)

  @Benchmark
  def printBooleansToByteBuffer: ByteBuffer = Printer.noSpaces.printToByteBuffer(booleansJson)

  @Benchmark
  def printIntsToString: String = Printer.noSpaces.print(intsJson)

  @Benchmark
  def printIntsToByteBuffer: ByteBuffer = Printer.noSpaces.printToByteBuffer(intsJson)
}
