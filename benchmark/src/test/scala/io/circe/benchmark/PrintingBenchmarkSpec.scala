package io.circe.benchmark

import argonaut.{ Json => JsonA, _ }, argonaut.Argonaut._
import org.scalatest.FlatSpec
import play.api.libs.json.{ Json => JsonP }

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

  it should "correctly print case classes using Circe" in {
    assert(decodeFoos(printFoosC) === Some(foos))
  }

  it should "correctly print case classes using Argonaut" in {
    assert(decodeFoos(printFoosA) === Some(foos))
  }

  it should "correctly print case classes using Play JSON" in {
    assert(decodeFoos(printFoosP) === Some(foos))
  }
}
