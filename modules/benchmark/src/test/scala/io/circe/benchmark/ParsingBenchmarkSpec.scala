package io.circe.benchmark

import org.scalatest.FlatSpec

class ParsingBenchmarkSpec extends FlatSpec {
  val benchmark: ParsingBenchmark = new ParsingBenchmark

  import benchmark._

  "The parsing benchmark" should "correctly parse integers using Circe" in {
    assert(parseIntsC === intsC)
  }

  it should "correctly parse integers using Circe with Jackson" in {
    assert(parseIntsCJ === intsC)
  }

  it should "correctly parse integers using Argonaut" in {
    assert(parseIntsA === intsA)
  }

  it should "correctly parse integers using Play JSON" in {
    assert(parseIntsP === intsP)
  }

  it should "correctly parse integers using Spray JSON" in {
    assert(parseIntsS === intsS)
  }

  it should "correctly parse integers using Picopickle" in {
    assert(parseIntsPico === intsPico)
  }

  it should "correctly parse case classes using Circe" in {
    assert(parseFoosC === foosC)
  }

  it should "correctly parse case classes using Circe with Jackson" in {
    assert(parseFoosCJ === foosC)
  }

  it should "correctly parse case classes using Argonaut" in {
    assert(parseFoosA === foosA)
  }

  it should "correctly parse case classes using Play JSON" in {
    assert(parseFoosP === foosP)
  }

  it should "correctly parse case classes using Spray JSON" in {
    assert(parseFoosS === foosS)
  }

  it should "correctly parse case classes using Picopickle" in {
    assert(parseFoosPico === foosPico)
  }
}
