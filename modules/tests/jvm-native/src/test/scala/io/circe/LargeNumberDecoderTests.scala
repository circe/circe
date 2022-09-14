package io.circe

import cats.instances.all._
import io.circe.parser.parse
import io.circe.tests.{ CirceMunitSuite, CirceSuite }
import org.scalacheck.Prop._

/**
 * Tests that fail because of bugs (or at least limitations) on Scala.js.
 */
trait LargeNumberDecoderTests { this: CirceMunitSuite =>
  property("Decoder[Long] should succeed on whole decimal values (#83)") {
    forAll { (v: Long, n: Byte) =>
      val zeros = "0" * (math.abs(n.toInt) + 1)
      val Right(json) = parse(s"$v.$zeros")
      Decoder[Long].apply(json.hcursor) ?= Right(v)
    }
  }

  property("Decoder[BigInt] should succeed on whole decimal values (#83)") {
    forAll { (v: BigInt, n: Byte) =>
      val zeros = "0" * (math.abs(n.toInt) + 1)
      val Right(json) = parse(s"$v.$zeros")
      Decoder[BigInt].apply(json.hcursor) ?= Right(v)
    }
  }
}

trait LargeNumberDecoderTestsMunit { this: CirceMunitSuite =>
  property("Decoder[Long] should succeed on whole decimal values (#83)") {
    forAll { (v: Long, n: Byte) =>
      val zeros = "0" * (math.abs(n.toInt) + 1)
      val Right(json) = parse(s"$v.$zeros")
      Decoder[Long].apply(json.hcursor) ?= Right(v)
    }
  }

  property("Decoder[BigInt] should succeed on whole decimal values (#83)") {
    forAll { (v: BigInt, n: Byte) =>
      val zeros = "0" * (math.abs(n.toInt) + 1)
      val Right(json) = parse(s"$v.$zeros")
      Decoder[BigInt].apply(json.hcursor) ?= Right(v)
    }
  }
}
