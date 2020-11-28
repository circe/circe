package io.circe.benchmark

import io.circe.Json
import cats.syntax.eq._
import munit.FunSuite
class InstantiationBenchmarkSpec extends FunSuite {
  val benchmark: InstantiationBenchmark = new InstantiationBenchmark

  import benchmark._

  test("decoderFromNew should correctly decode") {
    assert(decoderFromNew == Right("xyz"))
  }

  test("decoderFromSAM should correctly decode") {
    assert(decoderFromNew === Right("xyz"))
  }

  test("decoderFromInstance should correctly decode") {
    assert(decoderFromNew === Right("xyz"))
  }

  test("encoderFromNew should correctly encode") {
    assert(encoderFromNew === Json.obj("value" -> Json.fromString("abc")))
  }

  test("encoderFromSAM should correctly encode") {
    assert(encoderFromSAM === Json.obj("value" -> Json.fromString("abc")))
  }

  test("encoderFromInstance should correctly encode") {
    assert(encoderFromInstance === Json.obj("value" -> Json.fromString("abc")))
  }
}
