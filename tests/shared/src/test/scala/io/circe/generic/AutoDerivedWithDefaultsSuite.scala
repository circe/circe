package io.circe.generic

import algebra.Eq
import cats.data.{ NonEmptyList, Validated, Xor }
import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.auto.defaults._
import io.circe.tests.{ CodecTests, CirceSuite }
import io.circe.tests.examples._
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Prop.forAll
import shapeless.{ CNil, Witness }, shapeless.labelled.{ FieldType, field }

class AutoDerivedWithDefaultsSuite extends CirceSuite {
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

  case class WithDefaultsExample(i: Int, d: Double = 1.0, s: String = "")

  object WithDefaultsExample {
    implicit val eqWithDefaultsExample: Eq[WithDefaultsExample] = Eq.fromUniversalEquals
    implicit val arbitraryWithDefaultsExample: Arbitrary[WithDefaultsExample] =
      Arbitrary(
        for {
          i <- Arbitrary.arbitrary[Int]
          d <- Arbitrary.arbitrary[Double]
          s <- Arbitrary.arbitrary[String]
        } yield WithDefaultsExample(i, d, s)
      )
  }

  checkAll("Codec[Tuple1[Int]]", CodecTests[Tuple1[Int]].codec)
  checkAll("Codec[(Int, Int, Foo)]", CodecTests[(Int, Int, Foo)].codec)
  checkAll("Codec[Qux[Int]]", CodecTests[Qux[Int]].codec)
  checkAll("Codec[Foo]", CodecTests[Foo].codec)
  checkAll("Codec[OuterCaseClassExample]", CodecTests[OuterCaseClassExample].codec)
  checkAll("Codec[RecursiveAdtExample]", CodecTests[RecursiveAdtExample].codec)
  checkAll("Codec[RecursiveWithOptionExample]", CodecTests[RecursiveWithOptionExample].codec)
  checkAll("Codec[WithDefaultsExample]", CodecTests[WithDefaultsExample].codec)

  test("Decoder[Int => Qux[String]]") {
    check {
      forAll { (i: Int, s: String, j: Int) =>
        Json.obj(
          "a" -> Json.string(s),
          "j" -> Json.int(j)
        ).as[Int => Qux[String]].map(_(i)) === Xor.right(Qux(i, s, j))
      }
    }
  }

  test("Decoder[FieldType[Witness.`'j`.T, Int] => Qux[String]]") {
    check {
      forAll { (i: Int, s: String, j: Int) =>
        Json.obj(
          "i" -> Json.int(i),
          "a" -> Json.string(s)
        ).as[FieldType[Witness.`'j`.T, Int] => Qux[String]].map(
          _(field(j))
        ) === Xor.right(Qux(i, s, j))
      }
    }
  }

  test("Decoder[Qux[String] => Qux[String]]") {
    check {
      forAll { (q: Qux[String], i: Option[Int], a: Option[String], j: Option[Int]) =>
        val json = Json.obj(
          "i" -> Encoder[Option[Int]].apply(i),
          "a" -> Encoder[Option[String]].apply(a),
          "j" -> Encoder[Option[Int]].apply(j)
        )

        val expected = Qux[String](i.getOrElse(q.i), a.getOrElse(q.a), j.getOrElse(q.j))

        json.as[Qux[String] => Qux[String]].map(_(q)) === Xor.right(expected)
      }
    }
  }

  test("Generic instances should not interfere with base instances") {
    check {
      forAll { (is: List[Int]) =>
        val json = Encoder[List[Int]].apply(is)

        json === Json.fromValues(is.map(Json.int)) && json.as[List[Int]] === Xor.right(is)
      }
    }
  }

  test("Generic decoders should not interfere with defined decoders") {
    check {
      forAll { (xs: List[String]) =>
        val json = Json.obj("Baz" -> Json.fromValues(xs.map(Json.string)))

        Decoder[Foo].apply(json.hcursor) === Xor.right(Baz(xs): Foo)
      }
    }
  }

  test("Generic encoders should not interfere with defined encoders") {
    check {
      forAll { (xs: List[String]) =>
        val json = Json.obj("Baz" -> Json.fromValues(xs.map(Json.string)))

        Encoder[Foo].apply(Baz(xs): Foo) === json
      }
    }
  }

  test("Decoding with Decoder[CNil] should fail") {
    assert(Json.empty.as[CNil].isLeft)
  }

  test("Encoding with Encoder[CNil] should throw an exception") {
    intercept[RuntimeException](Encoder[CNil].apply(null: CNil))
  }

  test("Decoders configured to use defaults should use defaults") {
    check {
      forAll { (i: Int, d: Option[Double], s: Option[String]) =>
        val json = Json.fromFields(
          List("i" -> Json.int(i)) ++
            d.map(dv => "d" -> Json.numberOrNull(dv)) ++
            s.map(sv => "s" -> Json.string(sv))
        )

        val expected = (d, s) match {
          case (Some(dv), Some(sv)) => WithDefaultsExample(i, dv, sv)
          case (    None, Some(sv)) => WithDefaultsExample(i, s = sv)
          case (Some(dv),     None) => WithDefaultsExample(i, d = dv)
          case (    None,     None) => WithDefaultsExample(i)
        }

        json.as[WithDefaultsExample] === Xor.right(expected)
      }
    }
  }
}
