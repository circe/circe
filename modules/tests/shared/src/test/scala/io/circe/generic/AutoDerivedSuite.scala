package io.circe.generic

import cats.Eq
import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.auto._
import io.circe.tests.{ CodecTests, CirceSuite }
import io.circe.tests.examples._
import org.scalacheck.{ Arbitrary, Gen }
import shapeless.{ CNil, Witness }, shapeless.labelled.{ FieldType, field }
import shapeless.test.illTyped

class AutoDerivedSuite extends CirceSuite {
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

  checkLaws("Codec[Tuple1[Int]]", CodecTests[Tuple1[Int]].codec)
  checkLaws("Codec[(Int, Int, Foo)]", CodecTests[(Int, Int, Foo)].codec)
  checkLaws("Codec[Qux[Int]]", CodecTests[Qux[Int]].codec)
  checkLaws("Codec[Foo]", CodecTests[Foo].codec)
  checkLaws("Codec[OuterCaseClassExample]", CodecTests[OuterCaseClassExample].codec)
  checkLaws("Codec[RecursiveAdtExample]", CodecTests[RecursiveAdtExample].codec)
  checkLaws("Codec[RecursiveWithOptionExample]", CodecTests[RecursiveWithOptionExample].codec)

  "Decoder[Int => Qux[String]]" should "decode partial JSON representations" in forAll { (i: Int, s: String, j: Int) =>
    val result = Json.obj(
      "a" -> Json.fromString(s),
      "j" -> Json.fromInt(j)
    ).as[Int => Qux[String]].map(_(i))

    assert(result === Right(Qux(i, s, j)))
  }

  "Decoder[FieldType[Witness.`'j`.T, Int] => Qux[String]]" should "decode partial JSON representations" in {
    forAll { (i: Int, s: String, j: Int) =>
      val result = Json.obj(
        "i" -> Json.fromInt(i),
        "a" -> Json.fromString(s)
      ).as[FieldType[Witness.`'j`.T, Int] => Qux[String]].map(
         _(field(j))
      )

      assert(result === Right(Qux(i, s, j)))
    }
  }

  "Decoder[Qux[String] => Qux[String]]" should "decode patch JSON representations" in {
    forAll { (q: Qux[String], i: Option[Int], a: Option[String], j: Option[Int]) =>
      val json = Json.obj(
        "i" -> Encoder[Option[Int]].apply(i),
        "a" -> Encoder[Option[String]].apply(a),
        "j" -> Encoder[Option[Int]].apply(j)
      )

      val expected = Qux[String](i.getOrElse(q.i), a.getOrElse(q.a), j.getOrElse(q.j))

      assert(json.as[Qux[String] => Qux[String]].map(_(q)) === Right(expected))
    }
  }

  "A generically derived codec" should "not interfere with base instances" in forAll { (is: List[Int]) =>
    val json = Encoder[List[Int]].apply(is)

    assert(json === Json.fromValues(is.map(Json.fromInt)) && json.as[List[Int]] === Right(is))
  }

  it should "not be derived for Object" in {
    illTyped("Decoder[Object]")
    illTyped("Encoder[Object]")
  }

  it should "not be derived for AnyRef" in {
    illTyped("Decoder[AnyRef]")
    illTyped("Encoder[AnyRef]")
  }

  "Generic decoders" should "not interfere with defined decoders" in forAll { (xs: List[String]) =>
    val json = Json.obj("Baz" -> Json.fromValues(xs.map(Json.fromString)))

    assert(Decoder[Foo].apply(json.hcursor) === Right(Baz(xs): Foo))
  }

  "Generic encoders" should "not interfere with defined encoders" in forAll { (xs: List[String]) =>
    val json = Json.obj("Baz" -> Json.fromValues(xs.map(Json.fromString)))

    assert(Encoder[Foo].apply(Baz(xs): Foo) === json)
  }

  "Decoder[CNil]" should "fail" in {
    assert(Json.Null.as[CNil].isLeft)
  }

  "Encoder[CNil]" should "throw an exception" in {
    intercept[RuntimeException](Encoder[CNil].apply(null: CNil))
  }
}
