package io.circe

import cats.kernel.Eq
import cats.kernel.instances.all._
import cats.syntax.eq._
import io.circe._
import io.circe.syntax._
import io.circe.derivation._
import io.circe.testing.CodecTests
import io.circe.tests.CirceMunitSuite
import org.scalacheck.{ Arbitrary, Gen }

object UnwrappedDerivesSuite {
  // 'derives UnwrappedDecoder, UnwrappedEncoder' does not work for generics
  // Because the compiler created given would require a `UnwrappedDecoder[A]`, and that may not exist.
  // Best practice would be to hand write the given with the correct constraint.
  case class Box[A](a: A)
  object Box {
    given [A: Eq]: Eq[Box[A]] = Eq.by(_.a)
    given [A: Arbitrary]: Arbitrary[Box[A]] = Arbitrary(Arbitrary.arbitrary[A].map(Box(_)))

    given [A: Decoder]: Decoder[Box[A]] = UnwrappedDecoder.derived[Box[A]]
    given [A: Encoder]: Encoder[Box[A]] = UnwrappedEncoder.derived[Box[A]]
  }

  case class BoxC[A](a: A)
  object BoxC {
    given [A: Eq]: Eq[BoxC[A]] = Eq.by(_.a)
    given [A: Arbitrary]: Arbitrary[BoxC[A]] = Arbitrary(Arbitrary.arbitrary.map(BoxC(_)))

    given [A: Encoder: Decoder]: Codec[BoxC[A]] = UnwrappedCodec.derived[BoxC[A]]
  }

  case class Wub(x: Long) derives UnwrappedDecoder, UnwrappedEncoder
  object Wub {
    given Eq[Wub] = Eq.by(_.x)
    given Arbitrary[Wub] = Arbitrary(Arbitrary.arbitrary[Long].map(Wub(_)))
  }

  case class WubC(x: Long) derives UnwrappedCodec
  object WubC {
    given Eq[WubC] = Eq.by(_.x)
    given Arbitrary[WubC] = Arbitrary(Arbitrary.arbitrary[Long].map(WubC(_)))
  }
}

class UnwrappedDerivesSuite extends CirceMunitSuite {
  import UnwrappedDerivesSuite._

  checkAll("Codec[Wub]", CodecTests[Wub].codec)
  checkAll("Codec[Box[Long]]", CodecTests[Box[Long]].codec)
  checkAll("Codec[Box[Wub]]", CodecTests[Box[Wub]].codec)

  checkAll("Codec[WubC]", CodecTests[WubC].codec)

  checkAll("Codec[BoxC[Long]]", CodecTests[BoxC[Long]].codec)
  checkAll("Codec[BoxC[Wub]]", CodecTests[BoxC[WubC]].codec)

  test("Wub Encodes into Long") {
    val w = Wub(10)

    assertEquals(w.asJson, 10.asJson)
  }
  test("Box[Wub] Encodes into Long") {
    val w = Box(Wub(10))

    assertEquals(w.asJson, 10.asJson)
  }
}
