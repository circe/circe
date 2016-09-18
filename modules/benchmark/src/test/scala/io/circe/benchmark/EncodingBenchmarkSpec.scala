package io.circe.benchmark

import argonaut.Parse, argonaut.Argonaut._
import org.scalatest.FlatSpec
import play.api.libs.json.{ Json => JsonP }
import io.github.netvl.picopickle.backends.jawn.JsonPickler._

class EncodingBenchmarkSpec extends FlatSpec {
  val benchmark: EncodingBenchmark = new EncodingBenchmark

  import benchmark._

  private[this] def decodeInts(json: String): Option[List[Int]] =
    Parse.decodeOption[List[Int]](json)

  private[this] def decodeFoos(json: String): Option[Map[String, Foo]] =
    Parse.decodeOption[Map[String, Foo]](json)

  "The encoding benchmark" should "correctly encode integers using Circe" in {
    assert(decodeInts(encodeIntsC.noSpaces) === Some(ints))
  }

  it should "correctly encode integers using Argonaut" in {
    assert(decodeInts(encodeIntsA.nospaces) === Some(ints))
  }

  it should "correctly encode integers using Play JSON" in {
    assert(decodeInts(JsonP.prettyPrint(encodeIntsP)) === Some(ints))
  }

  it should "correctly encode integers using Spray JSON" in {
    assert(decodeInts(encodeIntsS.compactPrint) === Some(ints))
  }

  it should "correctly encode integers using Picopickle" in {
    assert(decodeInts(writeAst(encodeIntsPico)) === Some(ints))
  }

  it should "correctly encode case classes using Circe" in {
    assert(decodeFoos(encodeFoosC.noSpaces) === Some(foos))
  }

  it should "correctly encode case classes using Argonaut" in {
    assert(decodeFoos(encodeFoosA.nospaces) === Some(foos))
  }

  it should "correctly encode case classes using Play JSON" in {
    assert(decodeFoos(JsonP.prettyPrint(encodeFoosP)) === Some(foos))
  }

  it should "correctly encode case classes using Spray JSON" in {
    assert(decodeFoos(encodeFoosS.compactPrint) === Some(foos))
  }

  it should "correctly encode case classes using Picopickle" in {
    assert(decodeFoos(writeAst(encodeFoosPico)) === Some(foos))
  }
}
