package io.circe.benchmark

import org.scalatest.FlatSpec

class DecodingBenchmarkSpec extends FlatSpec {
  val benchmark: DecodingBenchmark = new DecodingBenchmark

  import benchmark._

  "The decoding benchmark" should "correctly decode integers using Circe" in {
    assert(decodeIntsC === ints)
  }

  it should "correctly decode integers using Argonaut" in {
    assert(decodeIntsA === ints)
  }

  it should "correctly decode integers using Play JSON" in {
    assert(decodeIntsP === ints)
  }

  it should "correctly decode integers using Spray JSON" in {
    assert(decodeIntsS === ints)
  }

  it should "correctly decode case classes using Circe" in {
    assert(decodeFoosC === foos)
  }

  it should "correctly decode case classes using Argonaut" in {
    assert(decodeFoosA === foos)
  }

  it should "correctly decode case classes using Play JSON" in {
    assert(decodeFoosP === foos)
  }

  it should "correctly decode case classes using Spray JSON" in {
    assert(decodeFoosS === foos)
  }
}
