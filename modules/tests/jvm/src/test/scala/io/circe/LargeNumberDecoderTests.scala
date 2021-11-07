package io.circe

import cats.instances.all._
import io.circe.parser.parse
import io.circe.tests.{ CirceMunitSuite, CirceSuite }
import org.scalacheck.Prop.forAll

/**
 * Tests that fail because of bugs (or at least limitations) on Scala.js.
 */
trait LargeNumberDecoderTests { this: CirceSuite =>
  "Decoder[Long]" should "succeed on whole decimal values (#83)" in forAll { (v: Long, n: Byte) =>
    val zeros = "0" * (math.abs(n.toInt) + 1)
    val Right(json) = parse(s"$v.$zeros")

    assert(Decoder[Long].apply(json.hcursor) === Right(v))
  }

  "Decoder[BigInt]" should "succeed on whole decimal values (#83)" in forAll { (v: BigInt, n: Byte) =>
    val zeros = "0" * (math.abs(n.toInt) + 1)
    val Right(json) = parse(s"$v.$zeros")

    assert(Decoder[BigInt].apply(json.hcursor) === Right(v))
  }
}

trait LargeNumberDecoderTestsMunit { this: CirceMunitSuite =>
  property("Decoder[Long] should succeed on whole decimal values (#83)") {
    forAll { (v: Long, n: Byte) =>
      val zeros = "0" * (math.abs(n.toInt) + 1)
      val Right(json) = parse(s"$v.$zeros")
      assertEquals(Decoder[Long].apply(json.hcursor), Right(v))
    }
  }

  property("Decoder[BigInt] should succeed on whole decimal values (#83)") {
    forAll { (v: BigInt, n: Byte) =>
      val zeros = "0" * (math.abs(n.toInt) + 1)
      val Right(json) = parse(s"$v.$zeros")
      assertEquals(Decoder[BigInt].apply(json.hcursor), Right(v))
    }
  }
}
