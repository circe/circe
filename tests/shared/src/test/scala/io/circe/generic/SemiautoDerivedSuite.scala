package io.circe.generic

import algebra.Eq
import cats.data.Xor
import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.decoding.DerivedDecoder
import io.circe.generic.encoding.DerivedObjectEncoder
import io.circe.generic.semiauto._
import io.circe.tests.{ CodecTests, CirceSuite }
import io.circe.tests.examples._
import org.scalacheck.{ Arbitrary, Gen }
import shapeless.{ CNil, Witness }, shapeless.labelled.{ FieldType, field }
import shapeless.test.illTyped

class SemiautoDerivedSuite extends CirceSuite {
  implicit def decodeQux[A: Decoder]: Decoder[Qux[A]] = deriveDecoder
  implicit def encodeQux[A: Encoder]: Encoder[Qux[A]] = deriveEncoder
  implicit val decodeWub: Decoder[Wub] = deriveDecoder
  implicit val encodeWub: Encoder[Wub] = deriveEncoder
  implicit val decodeFoo: Decoder[Foo] = deriveDecoder
  implicit val encodeFoo: Encoder[Foo] = deriveEncoder

  implicit val decodeIntlessQux: Decoder[Int => Qux[String]] =
    deriveFor[Int => Qux[String]].incomplete

  implicit val decodeJlessQux: Decoder[FieldType[Witness.`'j`.T, Int] => Qux[String]] =
    deriveFor[FieldType[Witness.`'j`.T, Int] => Qux[String]].incomplete

  implicit val decodeQuxPatch: Decoder[Qux[String] => Qux[String]] = deriveFor[Qux[String]].patch

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

    implicit val decodeRecursiveAdtExample: Decoder[RecursiveAdtExample] = deriveDecoder
    implicit val encodeRecursiveAdtExample: Encoder[RecursiveAdtExample] = deriveEncoder
  }

  case class RecursiveWithOptionExample(o: Option[RecursiveWithOptionExample])

  object RecursiveWithOptionExample {
    implicit val eqRecursiveWithOptionExample: Eq[RecursiveWithOptionExample] =
      Eq.fromUniversalEquals

    private def atDepth(depth: Int): Gen[RecursiveWithOptionExample] = if (depth < 3)
      Arbitrary.arbitrary[Option[RecursiveWithOptionExample]].map(
        RecursiveWithOptionExample(_)
      ) else Gen.const(RecursiveWithOptionExample(None))

    implicit val arbitraryRecursiveWithOptionExample: Arbitrary[RecursiveWithOptionExample] =
      Arbitrary(atDepth(0))

    implicit val decodeRecursiveWithOptionExample: Decoder[RecursiveWithOptionExample] =
      deriveDecoder

    implicit val encodeRecursiveWithOptionExample: Encoder[RecursiveWithOptionExample] =
      deriveEncoder
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

    implicit val encodeWithDefaultsExample: Encoder[WithDefaultsExample] = deriveEncoder
  }

  case class OvergenerationExampleInner(i: Int)
  case class OvergenerationExampleOuter0(i: OvergenerationExampleInner)
  case class OvergenerationExampleOuter1(oi: Option[OvergenerationExampleInner])

  checkAll("Codec[Tuple1[Int]]", CodecTests[Tuple1[Int]].codec)
  checkAll("Codec[(Int, Int, Foo)]", CodecTests[(Int, Int, Foo)].codec)
  checkAll("Codec[Qux[Int]]", CodecTests[Qux[Int]].codec)
  checkAll("Codec[Foo]", CodecTests[Foo].codec)
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

  test("Generic instances shouldn't come from nowhere") {
    implicitly[DerivedDecoder[OvergenerationExampleInner]]
    illTyped("Decoder[OvergenerationExampleInner]")

    implicitly[DerivedObjectEncoder[OvergenerationExampleInner]]
    illTyped("Encoder[OvergenerationExampleInner]")

    illTyped("Decoder[OvergenerationExampleOuter0]")
    illTyped("Encoder[OvergenerationExampleOuter0]")
    illTyped("Decoder[OvergenerationExampleOuter1]")
    illTyped("Encoder[OvergenerationExampleOuter1]")
  }

  test("Semi-automatic derivation should require explicit instances for all parts") {
    illTyped("deriveDecoder[OvergenerationExampleInner0]")
    illTyped("deriveDecoder[OvergenerationExampleInner1]")
    illTyped("deriveEncoder[OvergenerationExampleInner0]")
    illTyped("deriveEncoder[OvergenerationExampleInner1]")
  }

  test("Decoders configured to use defaults should use defaults") {
    /**
     * TODO: The tests currently fail with a stack overflow when this is defined in the companion
     * object, and I currently have no idea why.
     */
    implicit val decodeWithDefaultsExample: Decoder[WithDefaultsExample] =
      deriveFor[WithDefaultsExample].decoderWithDefaults

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
