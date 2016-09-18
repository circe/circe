package io.circe.benchmark

import cats.data.Xor
import org.scalatest.FlatSpec

class CirceDerivationBenchmarkSpec extends FlatSpec {
  val benchmark: CirceDerivationBenchmark = new CirceDerivationBenchmark

  import benchmark._

  "The derived codecs" should "correctly decode Foos" in {
    assert(decodeDerived === Xor.right(exampleFoo))
  }

  it should "correctly encode Foos" in {
    assert(encodeDerived === exampleFooJson)
  }

  "The non-derived codecs" should "correctly decode Foos" in {
    assert(decodeNonDerived === Xor.right(exampleFoo))
  }

  it should "correctly encode Foos" in {
    assert(encodeNonDerived === exampleFooJson)
  }
}
