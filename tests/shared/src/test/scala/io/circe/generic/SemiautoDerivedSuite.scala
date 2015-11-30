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
  implicit def decodeQux[A: Decoder]: Decoder[Qux[A]] = deriveFor[Qux[A]].decoder
  implicit def encodeQux[A: Encoder]: Encoder[Qux[A]] = deriveFor[Qux[A]].encoder
  implicit val decodeWub: Decoder[Wub] = deriveFor[Wub].decoder
  implicit val encodeWub: Encoder[Wub] = deriveFor[Wub].encoder
  implicit val decodeFoo: Decoder[Foo] = deriveFor[Foo].decoder
  implicit val encodeFoo: Encoder[Foo] = deriveFor[Foo].encoder

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

    implicit val decodeRecursiveAdtExample: Decoder[RecursiveAdtExample] =
      deriveFor[RecursiveAdtExample].decoder

    implicit val encodeRecursiveAdtExample: Encoder[RecursiveAdtExample] =
      deriveFor[RecursiveAdtExample].encoder
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
      deriveFor[RecursiveWithOptionExample].decoder

    implicit val encodeRecursiveWithOptionExample: Encoder[RecursiveWithOptionExample] =
      deriveFor[RecursiveWithOptionExample].encoder
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
}
