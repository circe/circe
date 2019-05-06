package io.circe.benchmark

import org.scalacheck.Properties
import org.typelevel.claimant.Claim

class FoldingBenchmarkSuite extends Properties("FoldingBenchmark") {
  val benchmark: FoldingBenchmark = new FoldingBenchmark

  property("withFoldWith should give the correct result") = Claim(
    benchmark.withFoldWith == 5463565
  )

  property("withFold should give the correct result") = Claim(
    benchmark.withFold == 5463565
  )

  property("withPatternMatch should give the correct result") = Claim(
    benchmark.withPatternMatch == 5463565
  )
}
