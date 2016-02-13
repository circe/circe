package io.circe

import cats.data.Xor
import io.circe.parser.parse
import io.circe.tests.CirceSuite

/**
 * Tests that fail because of bugs (or at least limitations) on Scala.js.
 */
trait LargeNumberDecoderTests { this: CirceSuite =>
  test("Decoder[Long] succeeds on whole decimal values (#83)") {
    check { (v: Long, n: Byte) =>
      val zeros = "0" * (math.abs(n.toInt) + 1)
      val Xor.Right(json) = parse(s"$v.$zeros")

      Decoder[Long].apply(json.hcursor) === Xor.right(v)
    }
  }

  test("Decoder[BigInt] succeeds on whole decimal values (#83)") {
    check { (v: BigInt, n: Byte) =>
      val zeros = "0" * (math.abs(n.toInt) + 1)
      val Xor.Right(json) = parse(s"$v.$zeros")

      Decoder[BigInt].apply(json.hcursor) === Xor.right(v)
    }
  }
}
