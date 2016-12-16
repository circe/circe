package io.circe.benchmark

import argonaut.Parse, argonaut.Argonaut._
import org.scalatest.FlatSpec

class PrintingBenchmarkSpec extends FlatSpec {
  val benchmark: PrintingBenchmark = new PrintingBenchmark

  import benchmark._

  private[this] def decodeInts(json: String): Option[List[Int]] =
    Parse.decodeOption[List[Int]](json)

  private[this] def decodeFoos(json: String): Option[Map[String, Foo]] =
    Parse.decodeOption[Map[String, Foo]](json)

  "The printing benchmark" should "correctly print integers using Circe" in {
    assert(decodeInts(printIntsC) === Some(ints))
  }

  it should "correctly print integers using Argonaut" in {
    assert(decodeInts(printIntsA) === Some(ints))
  }

  it should "correctly print integers using Play JSON" in {
    assert(decodeInts(printIntsP) === Some(ints))
  }

  it should "correctly print integers using Spray JSON" in {
    assert(decodeInts(printIntsS) === Some(ints))
  }

  it should "correctly print integers using Picopickle" in {
    assert(decodeInts(printIntsPico) === Some(ints))
  }

  it should "correctly print case classes using Circe" in {
    assert(decodeFoos(printFoosC) === Some(foos))
  }

  it should "correctly print case classes using Argonaut" in {
    assert(decodeFoos(printFoosA) === Some(foos))
  }

  it should "correctly print case classes using Play JSON" in {
    assert(decodeFoos(printFoosP) === Some(foos))
  }

  it should "correctly print case classes using Spray JSON" in {
    assert(decodeFoos(printFoosS) === Some(foos))
  }

  it should "correctly print case classes using Picopickle" in {
    assert(decodeFoos(printFoosPico) === Some(foos))
  }
}
