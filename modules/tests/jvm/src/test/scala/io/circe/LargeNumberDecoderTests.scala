package io.circe

import cats.data.Xor
import io.circe.parser.parse
import io.circe.tests.CirceSuite

/**
 * Tests that fail because of bugs (or at least limitations) on Scala.js.
 */
trait LargeNumberDecoderTests { this: CirceSuite =>
  "Decoder[Long]" should "succeed on whole decimal values (#83)" in forAll { (v: Long, n: Byte) =>
    val zeros = "0" * (math.abs(n.toInt) + 1)
    val Xor.Right(json) = parse(s"$v.$zeros")

    assert(Decoder[Long].apply(json.hcursor) === Xor.right(v))
  }

  "Decoder[BigInt]" should "succeed on whole decimal values (#83)" in forAll { (v: BigInt, n: Byte) =>
    val zeros = "0" * (math.abs(n.toInt) + 1)
    val Xor.Right(json) = parse(s"$v.$zeros")

    assert(Decoder[BigInt].apply(json.hcursor) === Xor.right(v))
  }
}
