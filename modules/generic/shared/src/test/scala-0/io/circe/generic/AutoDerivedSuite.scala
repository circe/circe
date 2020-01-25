package io.circe.generic

import cats.kernel.Eq
import cats.syntax.contravariant._
import cats.syntax.eq._
import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.auto._
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import io.circe.tests.examples._
import org.scalacheck.{ Arbitrary, Gen }

object AutoDerivedSuite {
  case class InnerCaseClassExample(a: String, b: String, c: String, d: String)
  case class OuterCaseClassExample(a: String, inner: InnerCaseClassExample)

  object InnerCaseClassExample {
    implicit val arbitraryInnerCaseClassExample: Arbitrary[InnerCaseClassExample] =
      Arbitrary(
        for {
          a <- Arbitrary.arbitrary[String]
          b <- Arbitrary.arbitrary[String]
          c <- Arbitrary.arbitrary[String]
          d <- Arbitrary.arbitrary[String]
        } yield InnerCaseClassExample(a, b, c, d)
      )
  }

  object OuterCaseClassExample {
    implicit val eqOuterCaseClassExample: Eq[OuterCaseClassExample] = Eq.fromUniversalEquals

    implicit val arbitraryOuterCaseClassExample: Arbitrary[OuterCaseClassExample] =
      Arbitrary(
        for {
          a <- Arbitrary.arbitrary[String]
          i <- Arbitrary.arbitrary[InnerCaseClassExample]
        } yield OuterCaseClassExample(a, i)
      )
  }
}

class AutoDerivedSuite extends CirceSuite {
  import AutoDerivedSuite._

  checkAll("Codec[Tuple1[Int]]", CodecTests[Tuple1[Int]].codec)
  checkAll("Codec[(Int, Int, Foo)]", CodecTests[(Int, Int, Foo)].codec)
  checkAll("Codec[Qux[Int]]", CodecTests[Qux[Int]].codec)
  checkAll("Codec[Seq[Foo]]", CodecTests[Seq[Foo]].codec)
  checkAll("Codec[Baz]", CodecTests[Baz].codec)
  checkAll("Codec[Foo]", CodecTests[Foo].codec)
  checkAll("Codec[OuterCaseClassExample]", CodecTests[OuterCaseClassExample].codec)

  "A generically derived codec" should "not interfere with base instances" in forAll { (is: List[Int]) =>
    val json = Encoder[List[Int]].apply(is)

    assert(json === Json.fromValues(is.map(Json.fromInt)) && json.as[List[Int]] === Right(is))
  }

  it should "not be derived for Object" in {
    assertTypeError("Decoder[Object]")
    assertTypeError("Encoder[Object]")
  }

  it should "not be derived for AnyRef" in {
    assertTypeError("Decoder[AnyRef]")
    assertTypeError("Encoder[AnyRef]")
  }

  "Generic decoders" should "not interfere with defined decoders" in forAll { (xs: List[String]) =>
    val json = Json.obj("Baz" -> Json.fromValues(xs.map(Json.fromString)))

    assert(Decoder[Foo].apply(json.hcursor) === Right(Baz(xs): Foo))
  }

  "Generic encoders" should "not interfere with defined encoders" in forAll { (xs: List[String]) =>
    val json = Json.obj("Baz" -> Json.fromValues(xs.map(Json.fromString)))

    assert(Encoder[Foo].apply(Baz(xs): Foo) === json)
  }
}
