package io.circe.benchmark

import org.scalatest.flatspec.AnyFlatSpec

class GenericDerivationBenchmarkSpec extends AnyFlatSpec {
  val benchmark: GenericDerivationBenchmark = new GenericDerivationBenchmark

  import benchmark._

  "The derived codecs" should "correctly decode Foos" in {
    assert(decodeDerived === Right(exampleFoo))
  }

  it should "correctly encode Foos" in {
    assert(encodeDerived === exampleFooJson)
  }

  "The non-derived codecs" should "correctly decode Foos" in {
    assert(decodeNonDerived === Right(exampleFoo))
  }

  it should "correctly encode Foos" in {
    assert(encodeNonDerived === exampleFooJson)
  }
}
