package io.circe.benchmark

import org.scalatest.FlatSpec

class FoldingBenchmarkSpec extends FlatSpec {
  val benchmark: FoldingBenchmark = new FoldingBenchmark

  "withFoldWith" should "give the correct result" in {
    assert(benchmark.withFoldWith === 5463565)
  }

  "withFold" should "give the correct result" in {
    assert(benchmark.withFold === 5463565)
  }

  "withPatternMatch" should "give the correct result" in {
    assert(benchmark.withPatternMatch === 5463565)
  }
}
