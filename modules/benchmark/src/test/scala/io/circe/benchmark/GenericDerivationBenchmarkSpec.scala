package io.circe.benchmark

import org.scalacheck.Properties
import org.typelevel.claimant.Claim

class GenericDerivationBenchmarkSuite extends Properties("GenericDerivationBenchmark") {
  val benchmark: GenericDerivationBenchmark = new GenericDerivationBenchmark

  import benchmark._

  property("The derived codecs should correctly decode Foos") = Claim(
    decodeDerived == Right(exampleFoo)
  )

  property("The derived codecs should correctly encode Foos") = Claim(
    encodeDerived == exampleFooJson
  )

  property("The non-derived codecs should correctly decode Foos") = Claim(
    decodeNonDerived == Right(exampleFoo)
  )

  property("The non-derived codecs should correctly encode Foos") = Claim(
    encodeNonDerived == exampleFooJson
  )
}
