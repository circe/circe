package io.circe.generic.extras

import cats.Eq
import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.tests.CirceSuite

object UnwrappedSemiautoDerivedSuite {
  case class Foo(value: String) extends AnyVal

  object Foo {
    implicit val eq: Eq[Foo] = Eq.fromUniversalEquals
    implicit val encoder: Encoder[Foo] = deriveUnwrappedEncoder
    implicit val decoder: Decoder[Foo] = deriveUnwrappedDecoder
  }
}

class UnwrappedSemiautoDerivedSuite extends CirceSuite {
  import UnwrappedSemiautoDerivedSuite._

  "Semi-automatic derivation" should "encode valueclasses" in forAll { (s: String) =>
    val foo = Foo(s)
    val expected = Json.fromString(s)

    assert(Encoder[Foo].apply(foo) === expected)
  }

  "Semi-automatic derivation" should "decode valueclasses" in forAll { (s: String) =>
    val json = Json.fromString(s)
    val expected = Right(Foo(s))

    assert(Decoder[Foo].decodeJson(json) === expected)
  }

  "Semi-automatic derivation" should "fail decoding incompatible json" in forAll { (i: Int, s: String) =>
    val json = Json.fromInt(i)
    val expected = Left(DecodingFailure("String", List()))

    assert(Decoder[Foo].decodeJson(json) === expected)
  }

}
