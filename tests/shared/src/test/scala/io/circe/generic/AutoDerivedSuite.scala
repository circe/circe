package io.circe.generic

import algebra.Eq
import cats.data.{ NonEmptyList, Validated, Xor }
import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.auto._
import io.circe.tests.{ CodecTests, CirceSuite }
import io.circe.tests.examples._
import org.scalacheck.{ Arbitrary, Gen }
import shapeless.{ CNil, Witness }, shapeless.labelled.{ FieldType, field }
import shapeless.test.illTyped

class AutoDerivedSuite extends CirceSuite {
  final case class InnerCaseClassExample(a: String, b: String, c: String, d: String)
  final case class OuterCaseClassExample(a: String, inner: InnerCaseClassExample)

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

  sealed trait RecursiveAdtExample
  case class BaseAdtExample(a: String) extends RecursiveAdtExample
  case class NestedAdtExample(r: RecursiveAdtExample) extends RecursiveAdtExample

  object RecursiveAdtExample {
    implicit val eqRecursiveAdtExample: Eq[RecursiveAdtExample] = Eq.fromUniversalEquals

    private def atDepth(depth: Int): Gen[RecursiveAdtExample] = if (depth < 3)
      Gen.oneOf(
        Arbitrary.arbitrary[String].map(BaseAdtExample(_)),
        atDepth(depth + 1).map(NestedAdtExample(_))
      ) else Arbitrary.arbitrary[String].map(BaseAdtExample(_))

    implicit val arbitraryRecursiveAdtExample: Arbitrary[RecursiveAdtExample] =
      Arbitrary(atDepth(0))
  }

  case class RecursiveWithOptionExample(o: Option[RecursiveWithOptionExample])

  object RecursiveWithOptionExample {
    implicit val eqRecursiveWithOptionExample: Eq[RecursiveWithOptionExample] =
      Eq.fromUniversalEquals

    private def atDepth(depth: Int): Gen[RecursiveWithOptionExample] = if (depth < 3)
      Gen.option(atDepth(depth + 1)).map(
        RecursiveWithOptionExample(_)
      ) else Gen.const(RecursiveWithOptionExample(None))

    implicit val arbitraryRecursiveWithOptionExample: Arbitrary[RecursiveWithOptionExample] =
      Arbitrary(atDepth(0))
  }

  checkAll("Codec[Tuple1[Int]]", CodecTests[Tuple1[Int]].codec)
  checkAll("Codec[(Int, Int, Foo)]", CodecTests[(Int, Int, Foo)].codec)
  checkAll("Codec[Qux[Int]]", CodecTests[Qux[Int]].codec)
  checkAll("Codec[Foo]", CodecTests[Foo].codec)
  checkAll("Codec[OuterCaseClassExample]", CodecTests[OuterCaseClassExample].codec)
  checkAll("Codec[RecursiveAdtExample]", CodecTests[RecursiveAdtExample].codec)
  checkAll("Codec[RecursiveWithOptionExample]", CodecTests[RecursiveWithOptionExample].codec)

  test("Decoder[Int => Qux[String]]") {
    check { (i: Int, s: String, j: Int) =>
      Json.obj(
        "a" -> Json.string(s),
        "j" -> Json.int(j)
      ).as[Int => Qux[String]].map(_(i)) === Xor.right(Qux(i, s, j))
    }
  }

  test("Decoder[FieldType[Witness.`'j`.T, Int] => Qux[String]]") {
    check { (i: Int, s: String, j: Int) =>
      Json.obj(
        "i" -> Json.int(i),
        "a" -> Json.string(s)
      ).as[FieldType[Witness.`'j`.T, Int] => Qux[String]].map(
         _(field(j))
      ) === Xor.right(Qux(i, s, j))
    }
  }

  test("Decoder[Qux[String] => Qux[String]]") {
    check { (q: Qux[String], i: Option[Int], a: Option[String], j: Option[Int]) =>
      val json = Json.obj(
        "i" -> Encoder[Option[Int]].apply(i),
        "a" -> Encoder[Option[String]].apply(a),
        "j" -> Encoder[Option[Int]].apply(j)
      )

      val expected = Qux[String](i.getOrElse(q.i), a.getOrElse(q.a), j.getOrElse(q.j))

      json.as[Qux[String] => Qux[String]].map(_(q)) === Xor.right(expected)
    }
  }

  test("Generic instances should not interfere with base instances") {
    check { (is: List[Int]) =>
      val json = Encoder[List[Int]].apply(is)

      json === Json.fromValues(is.map(Json.int)) && json.as[List[Int]] === Xor.right(is)
    }
  }

  test("Generic decoders should not interfere with defined decoders") {
    check { (xs: List[String]) =>
      val json = Json.obj("Baz" -> Json.fromValues(xs.map(Json.string)))

      Decoder[Foo].apply(json.hcursor) === Xor.right(Baz(xs): Foo)
    }
  }

  test("Generic encoders should not interfere with defined encoders") {
    check { (xs: List[String]) =>
      val json = Json.obj("Baz" -> Json.fromValues(xs.map(Json.string)))

      Encoder[Foo].apply(Baz(xs): Foo) === json
    }
  }

  test("Decoding with Decoder[CNil] should fail") {
    assert(Json.empty.as[CNil].isLeft)
  }

  test("Encoding with Encoder[CNil] should throw an exception") {
    intercept[RuntimeException](Encoder[CNil].apply(null: CNil))
  }

  /**
   * These tests currently fail on 2.10 even though these instances aren't available.
  test("Generic instances should not be derived for Object") {
    illTyped("Decoder[Object]")
    illTyped("Encoder[Object]")
  }

  test("Generic instances should not be derived for AnyRef") {
    illTyped("Decoder[AnyRef]")
    illTyped("Encoder[AnyRef]")
  }*/
}
