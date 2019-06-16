package io.circe.benchmark

import io.circe.Json
import org.scalatest.flatspec.AnyFlatSpec

class InstantiationBenchmarkSpec extends AnyFlatSpec {
  val benchmark: InstantiationBenchmark = new InstantiationBenchmark

  import benchmark._

  "decoderFromNew" should "correctly decode" in {
    assert(decoderFromNew === Right("xyz"))
  }

  "decoderFromSAM" should "correctly decode" in {
    assert(decoderFromNew === Right("xyz"))
  }

  "decoderFromInstance" should "correctly decode" in {
    assert(decoderFromNew === Right("xyz"))
  }

  "encoderFromNew" should "correctly encode" in {
    assert(encoderFromNew === Json.obj("value" -> Json.fromString("abc")))
  }

  "encoderFromSAM" should "correctly encode" in {
    assert(encoderFromSAM === Json.obj("value" -> Json.fromString("abc")))
  }

  "encoderFromInstance" should "correctly encode" in {
    assert(encoderFromInstance === Json.obj("value" -> Json.fromString("abc")))
  }
}
