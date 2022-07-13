package io.circe.benchmark

import munit.FunSuite
import cats.syntax.eq._

class FoldingBenchmarkSpec extends FunSuite {
  val benchmark: FoldingBenchmark = new FoldingBenchmark

  test("withFoldWith should give the correct result") {
    assert(benchmark.withFoldWith === 5463565)
  }

  test("withFold should give the correct result") {
    assert(benchmark.withFold === 5463565)
  }

  test("withPatternMatch should give the correct result") {
    assert(benchmark.withPatternMatch === 5463565)
  }
}
