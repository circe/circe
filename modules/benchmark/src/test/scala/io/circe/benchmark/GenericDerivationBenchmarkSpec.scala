package io.circe.benchmark

import munit.FunSuite
import cats.syntax.eq._

class GenericDerivationBenchmarkSpec extends FunSuite {
  val benchmark: GenericDerivationBenchmark = new GenericDerivationBenchmark

  import benchmark._

  test("The derived codecs should correctly decode Foos") {
    assert(decodeDerived === Right(exampleFoo))
  }

  test("The derived codecs should correctly encode Foos") {
    assert(encodeDerived === exampleFooJson)
  }

  test("The non-derived codecs should correctly decode Foos") {
    assert(decodeNonDerived === Right(exampleFoo))
  }

  test("the non-derived codecs should correctly encode Foos") {
    assert(encodeNonDerived === exampleFooJson)
  }
}
