package io.circe.generic.extras

import cats.Eq
import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Arbitrary.arbitrary

object UnwrappedSemiautoDerivedSuite {
  case class Foo(value: String)

  object Foo {
    implicit val eq: Eq[Foo] = Eq.fromUniversalEquals
    implicit val encoder: Encoder[Foo] = deriveUnwrappedEncoder
    implicit val decoder: Decoder[Foo] = deriveUnwrappedDecoder
    val fooGen: Gen[Foo] = arbitrary[String].map(Foo(_))
    implicit val arbitraryFoo: Arbitrary[Foo] = Arbitrary(fooGen)
  }
}

class UnwrappedSemiautoDerivedSuite extends CirceSuite {
  import UnwrappedSemiautoDerivedSuite._

  checkLaws("Codec[Foo]", CodecTests[Foo].codec)

  "Semi-automatic derivation" should "encode value classes" in forAll { (s: String) =>
    val foo = Foo(s)
    val expected = Json.fromString(s)

    assert(Encoder[Foo].apply(foo) === expected)
  }

  it should "decode value classes" in forAll { (s: String) =>
    val json = Json.fromString(s)
    val expected = Right(Foo(s))

    assert(Decoder[Foo].decodeJson(json) === expected)
  }

  it should "fail decoding incompatible JSON" in forAll { (i: Int, s: String) =>
    val json = Json.fromInt(i)
    val expected = Left(DecodingFailure("String", List()))

    assert(Decoder[Foo].decodeJson(json) === expected)
  }

}
