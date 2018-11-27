package io.circe.benchmark

import io.circe.generic.extras.Configuration
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

/**
  * Measure the performance of the case transformations.
  *
  * The following command will run the benchmarks with reasonable settings:
  *
  * > `sbt "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.KeyCasingBenchmark"`
  */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class KeyCasingBenchmark {

  @Benchmark
  def snakeCase: String = Configuration.snakeCaseTransformation("SnakeCase")

  @Benchmark
  def kebabCase: String = Configuration.kebabCaseTransformation("KebabCase")

}
