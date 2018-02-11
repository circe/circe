package io.circe.benchmark

import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

import io.circe.printer.Printer
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
  def printFoosToString: String = Printer.noSpaces.pretty(foosJson)

  @Benchmark
  def printFoosToByteBuffer: ByteBuffer = Printer.noSpaces.prettyByteBuffer(foosJson)

  @Benchmark
  def printHelloWorldToString: String = Printer.noSpaces.pretty(helloWorldJson)

  @Benchmark
  def printHelloWorldToByteBuffer: ByteBuffer = Printer.noSpaces.prettyByteBuffer(helloWorldJson)

  @Benchmark
  def printBooleansToString: String = Printer.noSpaces.pretty(booleansJson)

  @Benchmark
  def printBooleansToByteBuffer: ByteBuffer = Printer.noSpaces.prettyByteBuffer(booleansJson)

  @Benchmark
  def printIntsToString: String = Printer.noSpaces.pretty(intsJson)

  @Benchmark
  def printIntsToByteBuffer: ByteBuffer = Printer.noSpaces.prettyByteBuffer(intsJson)
}
