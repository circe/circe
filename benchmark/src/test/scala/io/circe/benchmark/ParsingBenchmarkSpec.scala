package io.circe.benchmark

import org.scalatest.FlatSpec

class ParsingBenchmarkSpec extends FlatSpec {
  val benchmark: ParsingBenchmark = new ParsingBenchmark

  import benchmark._

  "The parsing benchmark" should "correctly parse integers using Circe" in {
    assert(parseIntsC === intsC)
  }

  it should "correctly parse integers using Argonaut" in {
    assert(parseIntsA === intsA)
  }

  it should "correctly parse integers using Play JSON" in {
    assert(parseIntsP === intsP)
  }

  it should "correctly parse case classes using Circe" in {
    assert(parseFoosC === foosC)
  }

  it should "correctly parse case classes using Argonaut" in {
    assert(parseFoosA === foosA)
  }

  it should "correctly parse case classes using Play JSON" in {
    assert(parseFoosP === foosP)
  }
}
