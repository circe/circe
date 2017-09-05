package io.circe.generic.extras

import cats.Eq
import io.circe._
import io.circe.generic.extras.semiauto._
import io.circe.tests.CirceSuite

object ValueClassSemiautoDerivedSuite {
  case class Foo(value: String) extends AnyVal

  object Foo {
    implicit val eq: Eq[Foo] = Eq.fromUniversalEquals
    implicit val encoder: Encoder[Foo] = deriveValueClassEncoder
    implicit val decoder: Decoder[Foo] = deriveValueClassDecoder
  }
}

class ValueClassSemiautoDerivedSuite extends CirceSuite {
  import ValueClassSemiautoDerivedSuite._

  "Semi-automatic derivation" should "encode valueclasses" in forAll { (s: String) =>
    val foo = Foo(s)
    val expected = Json.fromString(s)

    assert(Encoder[Foo].apply(foo) === expected)
  }

  "Semi-automatic derivation" should "decode valueclasses" in forAll { (s: String) =>
    val json = Json.fromString(s)
    val expected = Foo(s)

    assert(Decoder[Foo].decodeJson(json) === Right(expected))
  }

}
