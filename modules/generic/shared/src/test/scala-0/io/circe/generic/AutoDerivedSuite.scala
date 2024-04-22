package io.circe.generic

import cats.kernel.Eq
import cats.syntax.contravariant._
import cats.syntax.eq._
import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.auto._
import io.circe.testing.CodecTests
import io.circe.tests.CirceMunitSuite
import io.circe.tests.examples._
import org.scalacheck.{ Arbitrary, Gen, Prop }

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

class AutoDerivedSuite extends CirceMunitSuite {
  import AutoDerivedSuite._

  checkAll("Codec[Tuple1[Int]]", CodecTests[Tuple1[Int]].codec)
  checkAll("Codec[(Int, Int, Foo)]", CodecTests[(Int, Int, Foo)].codec)
  checkAll("Codec[Qux[Int]]", CodecTests[Qux[Int]].codec)
  checkAll("Codec[Seq[Foo]]", CodecTests[Seq[Foo]].codec)
  checkAll("Codec[Baz]", CodecTests[Baz].codec)
  checkAll("Codec[Foo]", CodecTests[Foo].codec)
  checkAll("Codec[OuterCaseClassExample]", CodecTests[OuterCaseClassExample].codec)

  property("A generically derived codec should not interfere with base instances") {
    Prop.forAll { (is: List[Int]) =>
      val json = Encoder[List[Int]].apply(is)

      assert(json === Json.fromValues(is.map(Json.fromInt)) && json.as[List[Int]] === Right(is))
    }
  }

  test("A generically derived codec should not be derived for Object") {
    assertTypeError("Decoder[Object]")
    assertTypeError("Encoder[Object]")
  }

  test("A generically derived codec should not be derived for AnyRef") {
    assertTypeError("Decoder[AnyRef]")
    assertTypeError("Encoder[AnyRef]")
  }

  property("Generic decoders should not interfere with defined decoders") {
    Prop.forAll { (xs: List[String]) =>
      val json = Json.obj("Baz" -> Json.fromValues(xs.map(Json.fromString)))

      assert(Decoder[Foo].apply(json.hcursor) === Right(Baz(xs): Foo))
    }
  }

  property("Generic encoders should not interfere with defined encoders") {
    Prop.forAll { (xs: List[String]) =>
      val json = Json.obj("Baz" -> Json.fromValues(xs.map(Json.fromString)))

      assert(Encoder[Foo].apply(Baz(xs): Foo) === json)
    }
  }
}
